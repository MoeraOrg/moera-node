# Media storage

[Architecture index](../architecture.md) · [Media ownership and workflows](media.md)

## Content and copies

Content identity is an unpadded Base64URL SHA-1 hash; a SHA-256 digest is stored separately for integrity/signing.
[MediaOperations.putInPlace()](../../src/main/java/org/moera/node/media/MediaOperations.java) ingests content in an
independent transaction under a media-hash database advisory lock. It reuses existing content or moves/probes new bytes
before publishing the row. A failure can leave orphan bytes: filesystem changes cannot roll back with PostgreSQL.

`fileName` and `cloudFileName` independently record exact local and cloud locations. Both copies can exist, and metadata
can outlive unavailable bytes. Never reconstruct an existing name from its hash and current extension rules.
`openContent()` uses a named local copy, or stages cloud-only content temporarily; it does not automatically repair a
named but missing local file from S3.

Resumable uploads are staging, not content ownership. A later private upload consumes completed staging through normal
ingest; staging has independent expiry. Chunk serialization is process-local. Supported media categories and processing
eligibility belong in `MimeUtil`; for example, SVG stays a private document without image processing.

## Serving

Public routes address exposed content by hash. Private routes address a tenant's owner UUID and require an unrestricted
owner, admin `VIEW_CONTENT`, or valid grant. Remote grants also validate their lease; byte serving applies malware
policy. Unauthorized private lookup appears missing. See
[MediaController](../../src/main/java/org/moera/node/rest/MediaController.java) and
[grant components](../../src/main/java/org/moera/node/media/grant).

Controller offload (`stream`, `sendfile`, `accel`) and direct serving (`none`, `filesystem`, `aws-s3`) are independent
choices. Offload preserves application authorization while delegating bytes. Direct URLs are capabilities issued after
authorization and remain usable according to their expiry/storage policy.

[DirectServeOperations](../../src/main/java/org/moera/node/media/DirectServeOperations.java) owns path creation and
refresh: filesystem paths use the persisted name and proxy-enforced HMAC/expiry; S3 paths are absolute presigned URLs.
These HMAC URLs and node-signed media grants use different credentials/contracts. Preserve absolute S3 URLs when
resolving paths, validate cached bucket/key identity before re-signing, and fall back to controller URLs when needed.

## S3 migration

`AwsS3MediaStorage` owns SDK transfer/credentials/presigning;
[AwsS3MediaUploader](../../src/main/java/org/moera/node/media/awss3/AwsS3MediaUploader.java) owns claims/publication:

1. A short `FOR UPDATE SKIP LOCKED` transaction claims a file and stores a renewable deadline as fencing token.
2. Upload runs outside the transaction; exact compare-and-set renewal retains the claim.
3. Publication requires that token still match and no compression job have appeared. It stores the exact key,
   invalidates caches, and clears the lease. Exposed content keeps local bytes; other content drops its local copy.

Retry uses the same deterministic key; a stale worker cannot publish after losing its claim. Upload and database
publication remain separate effects, and local deletion during publication cannot be rolled back. Claim queries live
in [MediaFileRepository](../../src/main/java/org/moera/node/data/MediaFileRepository.java).

## Cleanup

Database triggers maintain reference counts and deferred deletion deadlines. New use clears the deadline. Cleanup first
removes eligible unused owners and atomically replaces unused content rows with `media_file_removals` tombstones
containing exact storage names; this phase does no external I/O.

[MediaCleanupOperations](../../src/main/java/org/moera/node/media/MediaCleanupOperations.java) then removes bytes.
Local deletion takes the ingest hash lock and preserves files for a recreated content row. Cloud deletion removes the
exact old key even if the hash was recreated, and waits while S3 is disabled. The tombstone disappears only after all
applicable removals succeed; failures remain retryable. The worker's overlap guard is process-local.

Schema: [V295](../../src/main/resources/db/migration/V295__media_file_names.sql).
[MediaOperationsTest](../../src/test/java/org/moera/node/media/MediaOperationsTest.java) covers copy/deduplication and
removal/recreation behavior.

## Deployment

The media root must be writable and persistent; it also holds transfer and resumable-upload staging. Runtime processing
needs `ffprobe`/`ffmpeg` with H.264 (`libx264`) and AAC support. Proxy configuration must implement selected offload and
filesystem signature contracts, including a shared direct-serving secret.

S3 requires an externally provisioned private bucket, region, credentials, object put/get/delete and multipart-abort
permissions, and lifecycle cleanup for incomplete uploads. Storage changes must preserve access to tombstone keys.
Exact properties belong in [MediaConfig](../../src/main/java/org/moera/node/config/MediaConfig.java),
[DirectServeConfig](../../src/main/java/org/moera/node/config/DirectServeConfig.java), and node options.
