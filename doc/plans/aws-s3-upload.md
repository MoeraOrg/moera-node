# AWS S3 Direct Serving Plan

## Goal

Add `DirectServeSource.AWSS3`. When this source is selected, a scheduled worker copies eligible local media files to a
configured Amazon S3 bucket in bounded batches. `MediaFile.fileName` continues to identify the local copy, while
`MediaFile.cloudFileName` identifies the S3 object.

For private media, expose separate direct URLs for inline viewing and downloading. S3 signs response-header overrides
as part of the URL, so the same presigned URL cannot safely be changed from inline display to attachment download by
appending `download=true`. Public media, previews, and avatars expose display direct paths only.

An S3 copy is eligible only when all of the following are true:

- `cloudFileName` is null;
- `fileName` is not null;
- `usageCount > 0`;
- `createdAt <= now - 30 minutes`;
- `recognizeAt` is null or `recognizedAt` is not null.

The local copy is retained after upload. This change does not introduce cloud-only media reads for OCR, preview
generation, application serving, or node-to-node transfers.

## Object Naming And Invariants

Use this S3 object key:

```text
<MediaFile.id>_<MediaFile.createdAt as decimal Unix seconds>.<MimeUtil extension for mimeType>
```

For example:

```text
AbCdEf0123_1784883600.jpg
```

Use `MediaFile.createdAt`, truncated to seconds, rather than the time of an individual upload attempt. This makes the
key stable across retries. If S3 accepts a PUT but the database publication later fails, the next attempt
overwrites the same key instead of leaking another object.

The creation timestamp also distinguishes different generations of a `MediaFile` with the same content hash. A
recreated row must wait at least 30 minutes before upload, so its key cannot collide with the key recorded by an older
removal tombstone. Therefore:

- an S3 deletion always targets the exact `cloudFileName` copied into `MediaFileRemoval`;
- recreating the same media hash does not prevent deletion of the old cloud object;
- cloud deletion does not need the media-ID advisory lock used to protect a reused filesystem name;
- `cloudFileName` is set only after a successful PUT;
- an already absent S3 object counts as successfully deleted;
- no S3 `HEAD`, bucket scan, or periodic existence reconciliation is needed.

Keep `cloudFileName` at its existing maximum length. Compared with the already bounded local `id.extension` form, the
cloud key adds one underscore and the current Unix-seconds value, so it fits within the existing `varchar(65)`.

Add one nullable `Timestamp cloudUploadDeadline` field to `MediaFile`. Null means unclaimed, a future value is an
active lease, and a past value is a stale lease that another worker may recover. Map it with a logic-free
getter/setter and rely on the project's automatic camel-case-to-snake-case mapping rather than adding a redundant
`@Column` annotation.

The exact stored deadline also acts as the fencing value. Every heartbeat, completion, and failure update must compare
the deadline value returned by the preceding claim or heartbeat, not merely test that the lease has not expired. A
worker whose lease has been recovered therefore cannot clear or publish over the new owner's lease.

Do not persist a separate provisional object key. Recalculate it from the persisted `id`, `createdAt`, and `mimeType`
for every retry; these naming inputs must remain immutable after media creation. Treat the MIME-to-extension mapping
used by this key format as backward-compatible: changing the extension for an existing MIME type could otherwise give
a post-crash retry a different key and leave the old object orphaned.

## AWS SDK Dependency

Use AWS SDK for Java 2.x and import its BOM through `dependencyManagement`. Pin a reviewed SDK version in a Maven
property (`2.47.3` is current when this plan is written), and add only the S3 module:

```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
</dependency>
```

The `s3` module contains `S3AsyncClient`, `S3Presigner`, S3 URI utilities, multipart support, and the request types
needed here. Do not add the whole `aws-sdk-java` aggregate, S3 Transfer Manager, AWS CRT, or unrelated service modules.

Configure the standard Java-based `S3AsyncClient` with `multipartEnabled(true)`. Current AWS SDK 2.x versions then make
a normal asynchronous `putObject` multipart-capable and upload large files in parallel parts. This is sufficient for
media that may reach 150–200 MB without the higher-level Transfer Manager or another native/runtime dependency. The
worker may wait for the returned future outside a database transaction; “wait synchronously” at the worker level does
not mean that S3 receives one monolithic PUT.

## Configuration

Extend `DirectServeConfig` with plain fields and logic-free getters/setters for:

- `bucket` (`String`): required for `AWSS3`;
- `region` (`String`): AWS region containing the bucket, required for `AWSS3`;
- `profile` (`String`, optional): name of a profile in the standard shared AWS config/credentials files.

Example:

```yaml
node:
  media:
    direct-serve:
      source: aws-s3
      bucket: example-moera-media
      region: eu-central-1
      profile: moera-production
```

Add `AWSS3` to `DirectServeSource` and verify that Spring's relaxed enum binding accepts the documented `aws-s3`
spelling. `secret` remains the filesystem reverse-proxy HMAC secret and is ignored by `AWSS3`.

Do not add access-key or secret-key fields. If `profile` is non-empty, use `ProfileCredentialsProvider` with that exact
profile name, so an operator can select one of several profiles in the standard shared AWS configuration. An explicitly
selected profile must take precedence over environment credentials rather than merely changing the profile consulted
late in the default chain. If `profile` is empty, use the standard AWS credentials provider chain so deployments can
use environment variables, the default profile, web identity, ECS credentials, or instance roles without storing
credentials in the node configuration.

Validate the S3-specific settings when the S3 component is initialized:

- bucket and region must be non-empty.

Keep the bucket/region/profile settings available even if `source` is temporarily changed away from `AWSS3`, so queued
`MediaFileRemoval` records can still be cleaned up. Missing or invalid S3 configuration must retain such tombstones and
produce a clear warning rather than silently discarding them.

## S3 Integration Layer

Introduce a small `AwsS3MediaStorage` component that hides SDK types from media-domain code. It should:

- own one long-lived, thread-safe multipart-enabled `S3AsyncClient` and one `S3Presigner`;
- use the configured region and bucket, plus the selected profile provider or default credentials provider chain;
- close the client and presigner during application shutdown;
- upload a `Path` through `S3AsyncClient.putObject(...)` with an `AsyncRequestBody` backed by the file, the media MIME
  type, and known content length;
- use automatic multipart upload for large objects and retry failed parts rather than restarting the entire file;
- delete an exact key idempotently;
- presign an exact key for GET;
- recover the bucket and key from a previously generated absolute S3 URL using the SDK's S3 URI parser.

Do not construct an SDK client or credentials provider for every API response.

Wrap this component behind methods that are easy to fake in unit tests. Tests must not require an AWS account or make
network calls.

## Direct-Serving Service

The current static `MediaUtil.directPath(...)` implementation can sign filesystem paths using only configuration, but
S3 signing needs the long-lived presigner. Introduce a `DirectServeOperations` component and move backend selection and
direct-path creation into it:

- `NONE`: return no display or download direct path;
- `FILESYSTEM`: use `MediaFile.fileName`, preserve the existing HMAC format and expiration behavior, and construct the
  download variant with the existing `download=true` reverse-proxy parameter on the server;
- `AWSS3`: use `MediaFile.cloudFileName`; if it is null, return no direct path; otherwise return an absolute S3
  presigned GET URL for inline display and a separate absolute presigned GET URL for attachment download.

Pass `DirectServeOperations` through the model builders and `ServeContext` where `DirectServeConfig` is currently passed
only to produce direct paths. This is a mechanical but broad refactor across the avatar, posting, comment, story,
reaction, private/public media, preview, event, and push model builders. Keep configuration access in the service and
do not put runtime clients into `DirectServeConfig`.

For S3, derive the signature duration from the `ExtendedDuration` already passed to `directPath(...)`. Do not add a
separate lifetime setting to `DirectServeConfig`. Calculate the S3 expiration as:

```text
min(expiration requested by ExtendedDuration, now + 7 days)
```

Seven days is a provider constraint of S3 SigV4 presigning, not an operator setting. This preserves shorter requested
lifetimes, including the existing three-day private-media lifetime, and converts `ExtendedDuration.ALWAYS`
public/avatar paths into the longest S3-supported lifetime. Return the exact resulting Unix expiration in the
corresponding field; the download field exists only for private media. Filesystem direct paths keep their current
long-lived `ALWAYS` behavior.

For private media, generate both S3 URLs from the same object key but different `GetObjectRequest` values:

- display: no attachment override; when a title-derived user filename is useful, an inline content-disposition
  override may preserve that filename without forcing a download;
- download: set `response-content-disposition` to an RFC-compatible `attachment` value containing the desired
  filename. For private media use `MediaFileOwner.getUserFileName()`, which selects the title or owner UUID and adds
  the MIME extension.

Build the disposition value with the existing Spring `ContentDisposition` support so quotes, non-ASCII characters, and
other filename characters are encoded safely. Do not hand-build this header.

Return independent expiration fields for the two private-media URLs. They will normally be identical because both are
generated together, but each URL must remain self-describing and refreshable:

- `directPathExpiresAt`;
- `directDownloadPathExpiresAt`.

For cached attachment paths:

- refresh filesystem URLs as today;
- for an absolute S3 URL, use `S3Utilities.parseUri(...)` to recover the decoded bucket and object key;
- accept it for refresh only when it is an HTTPS URL without user-info or a fragment, the parsed bucket exactly equals
  the configured bucket, and the flat key matches the expected
  `<requested media ID>_<decimal createdAt seconds>.<non-empty extension>` form;
- reject a URL for another bucket, a key for another media ID, a malformed/non-S3 URL, or a relative filesystem URL
  while `AWSS3` is selected;
- rebuild both display and download `GetObjectRequest` values from the validated key; recompute response content
  disposition from the cached media title/MIME data passed by the caller rather than trusting the old URL;
- never copy old `X-Amz-*` signing parameters or arbitrary query parameters into the new request;
- never derive an S3 key from the media hash, MIME type, or current time during refresh.

This validation is entirely local. Do not verify the old signature, call `HEAD`, or check that the object currently
exists. If validation fails, return no direct paths and let the ordinary controller path remain the fallback. A cached
display URL is sufficient to recover the validated object key and refresh both URL variants, which also upgrades old
cache documents that predate `directDownloadPath`.

## Absolute Direct URLs

The node API currently documents direct paths as relative to `/media`, and several server and web-client call sites
unconditionally prepend that prefix. An S3 presigned URL is absolute, so update the contract for both `directPath` and
`directDownloadPath` to allow either:

- a relative direct path for `FILESYSTEM`; or
- an absolute direct URL for `AWSS3`.

Add one Java helper that resolves either kind of direct path against `/moera/media/` only when the value is relative.
Use it instead of string concatenation in at least:

- `LocalRemoteMedia`;
- `LocalRemoteMediaInfo`;
- both `MediaUtil.mediaSources(...)` variants;
- `LinkPreviewHelper`;
- `GalleriesHelperSource`;
- any other server-rendered `src`, `srcset`, or link that consumes `directPath`.

Add the equivalent helper to `moera-client-react/src/util/media-images.ts` and use it for avatars, media images,
previews, `srcset`, gallery/lightbox links, copied HTML, search previews, and other direct-path consumers.

For a download, clients must:

1. Use `directDownloadPath` unchanged when it is present, resolving it as absolute or relative.
2. Otherwise use the ordinary media controller `path` with `download=true`.
3. Never use `directPath` as a download URL and never append `download=true` to a direct URL.

Keep stable “copy download link” actions on the ordinary controller path because a presigned direct download URL
expires. Update `mediaDownloadUrl(...)`, `EntryFile`, `MediaDownloadButton`, and the lightbox so display and download
URLs are passed separately. Android save integrations should receive the resolved direct download URL when available.

## API Schema And Generated Consumers

`DirectServeSource` itself is an internal configuration enum and does not belong in the node API schema.

In `../moeraorg.github.io/_data/node_api.yml`:

- change every `directPath` description from “relative to `/media`” to “relative to `/media` or an absolute direct
  URL”;
- add optional `directDownloadPath` and `directDownloadPathExpiresAt` fields only to `PrivateMediaFileInfo`;
- document `directDownloadPath` as a relative or absolute location that returns the original media with attachment
  content disposition;
- do not add these fields to `PublicMediaFileInfo`, `MediaFilePreviewInfo`, or avatar structs. Public media and previews
  continue to use their ordinary controller paths for explicit downloads.

Use `type: timestamp` for `directDownloadPathExpiresAt`. Then follow the normal API workflow:

1. Edit the schema source first.
2. Run `../update-api` once.
3. Rebuild `../java-moeralib` with `mvn clean install`.
4. Fix handwritten consumers, especially `../moera-client-react/src`, to resolve absolute URLs as described above.
5. Compile `moera-node`.

Regeneration updates the affected struct fingerprints and generated models. Do not hand-edit generated library files.
The semantic expansion must also be documented because old clients that always prepend `/media/` cannot consume S3
URLs correctly.

## Upload Candidate Query And Index

Add explicit repository queries, as required by the project conventions:

1. Recover an expired lease first (`cloud_upload_deadline < current_timestamp`) by replacing it with a new future
   deadline.
2. Otherwise select an unclaimed eligible row (`cloud_upload_deadline IS NULL`) with `FOR UPDATE SKIP LOCKED`, ordered
   by `created_at` and `id`, and set its lease deadline.
3. Return an immutable claim snapshot containing the media ID, exact stored deadline, persisted filesystem name, MIME
   type, size, creation time, and calculated S3 object key, then commit immediately.

The claim transaction may briefly take a row lock to serialize the state transition, but no transaction, row lock, or
database connection remains open during S3 network I/O. `SKIP LOCKED` lets multiple application instances claim
different rows without waiting for each other.

Change the explicit candidate query in `MediaFileRepository.moveUnusedToRemovals()` to require
`cloud_upload_deadline IS NULL`. The durable lease, rather than a long row lock, then protects the local file and
database row from purge during upload. Claiming and purging may contend only for the short state-transition
transaction.

Add a Flyway migration, expected to be `V296__media_file_cloud_upload_lease.sql`, that:

- adds a nullable `cloud_upload_deadline timestamp without time zone` column, matching the project's existing
  `Timestamp` mapping;
- adds a partial candidate index beginning with `created_at` and covering only rows where:

```sql
cloud_file_name IS NULL
AND cloud_upload_deadline IS NULL
AND file_name IS NOT NULL
AND usage_count > 0
AND (recognize_at IS NULL OR recognized_at IS NOT NULL)
```

- adds a partial index on `cloud_upload_deadline` where
  `cloud_file_name IS NULL AND cloud_upload_deadline IS NOT NULL`, for efficient expired-lease recovery.

Do not edit generated `doc/create_tables.sql`.

## Scheduled Upload Worker

Add a scheduled method, either in a focused `AwsS3MediaOperations` component or in `MediaOperations`, with:

- a fixed delay of five minutes;
- a batch size of 100;
- one batch per invocation;
- a process-local `AtomicBoolean` overlap guard;
- the usual `RequestCounter.allot()` scope and summary/error logging;
- creation of new leases only while the selected source is `AWSS3`.

Existing expired leases must remain recoverable even if the direct-serve source is later changed, provided the S3
configuration is still available. This prevents an abandoned lease from blocking purge forever. If the S3
configuration or credentials are unavailable, retain the lease, log the condition, and retry later; do not clear it
blindly because the object may already exist.

Process one durable lease at a time, up to the per-run batch limit:

1. Acquire or recover a lease in the short transaction described above.
2. Outside any transaction, resolve and validate the snapshotted local path through `MediaOperations.getPath(...)`;
   never reconstruct the local name.
3. Start the multipart-capable asynchronous `putObject` with the media `Content-Type`, known content length, and
   file-backed request body. Do not read the entire object into heap memory.
4. While the upload future is running, renew `cloud_upload_deadline` periodically with a short conditional update
   matching both the media ID and the exact previous deadline. The update must replace it with a later deadline and
   return that exact stored value for the next compare-and-set. Use a lease substantially longer than the heartbeat
   interval (for example, a 15-minute lease renewed every five minutes).
5. Retry transient heartbeat failures while the current lease still has a safety margin. If ownership cannot be
   confirmed before expiry, or a heartbeat reports that the expected deadline no longer matches, cancel/abort the
   upload where possible and never publish its result.
6. After S3 reports success, use a second short transaction with a conditional query matching the media ID and exact
   current deadline. Set `cloudFileName` to the deterministic key, clear `cloudUploadDeadline`, clear attachment
   caches that refer to any owner of the media file, and commit. Do not rebuild the caches in the worker; the next
   request recreates them with the newly available S3 direct path.
7. If upload fails, abort the multipart upload where possible and, only if the expected deadline still matches, move
   it forward using the retry backoff. A later expired-lease recovery recalculates the same deterministic key.

If `usageCount` drops to zero after claiming, let an already completed upload publish its key; the later purge
transaction can then place the exact key in the tombstone. Never discard a successfully uploaded object without first
recording its key durably.

Crash recovery is idempotent:

- a crash before or during upload leaves a lease that eventually expires;
- a crash after S3 completed but before database publication causes the recovered worker to upload to the same
  deterministically calculated key again and then publish it;
- a stale worker cannot complete or mutate a lease after its expected deadline has been replaced;
- abandoned multipart parts are cleaned by the bucket lifecycle rule.

Add an explicit bulk query such as `EntryRevisionRepository.clearAttachmentsCacheByMediaFile(String mediaFileId)`.
Without this invalidation, an existing attachment cache whose `directPath` is null would never discover the newly
uploaded cloud copy. Run it only as part of successful upload publication, in the same short transaction that sets
`cloudFileName`; a failed upload must leave existing caches unchanged.

Record the selected direct-serve source in `MediaAttachmentsCache`. Treat a cache created for another source as stale
and rebuild it. This handles configuration transitions between filesystem and S3; per-media invalidation handles the
normal null-to-uploaded transition while the source remains `AWSS3`. Populate and refresh both direct URL variants in
`PrivateMediaFileInfoUtil`. `PublicMediaFileInfoUtil` and `MediaFilePreviewInfoUtil` continue to populate display direct
paths only.

## Cloud Deletion

Complete the existing cloud-removal TODO in `MediaOperations.removeMediaFile(...)`, but separate local and cloud race
rules:

- local deletion still takes the `media-file:` advisory lock and skips the recorded local name if a `MediaFile` with
  the same hash has been recreated;
- S3 deletion does not take that lock and does not skip deletion merely because the hash was recreated;
- S3 deletion always uses the exact `MediaFileRemoval.cloudFileName`;
- do not hold the advisory lock or a database transaction while calling S3.

The removal flow should be:

1. Load the tombstone snapshot.
2. In a short transaction, acquire the media-ID lock and either delete the exact local file or mark local cleanup
   logically complete because the media ID was recreated.
3. Outside that lock/transaction, delete the exact S3 key. Treat `NoSuchKey`/already absent as success.
4. In a final transaction, delete the tombstone only when every recorded copy was deleted or safely skipped.

If any required deletion fails, retain the tombstone. Retrying local and S3 deletion is idempotent. If a recreated row
exists, retain its potentially reused local file but still remove the old timestamped S3 object.

Multiple node instances may process the same tombstone. Exact-key S3 deletes and local `deleteIfExists` are idempotent;
the final tombstone delete must also tolerate another worker completing it first.

## Documentation And Operations

Update `doc/media-files.md` with:

- the `aws-s3` source and all three new properties;
- the eligibility delay and OCR condition;
- deterministic object naming;
- finite absolute display URLs and private-media download presigned URLs;
- download URLs signed with attachment content disposition and their fallback behavior;
- local-copy retention;
- S3 deletion semantics.

Document that the bucket is provisioned outside the application and should be private. The node identity needs only
the relevant object permissions on the configured bucket/prefix:

- `s3:PutObject`;
- `s3:GetObject`;
- `s3:DeleteObject`;
- `s3:AbortMultipartUpload`.

Document named-profile selection and the default credentials provider chain. Recommend a role/web-identity credential
source in deployed environments. Also note that a presigned URL cannot remain usable after the temporary credentials
that signed it expire, even when the `ExtendedDuration` request and seven-day S3 cap would allow a later expiration.

Require or strongly recommend an S3 bucket lifecycle rule with `AbortIncompleteMultipartUpload` (for example, after
seven days). This is the safety net for uploaded parts left behind if the process or host dies before the SDK can
complete or abort the upload.

## Tests

Add focused tests for:

- Spring binds `source: aws-s3`, bucket, region, and an optional profile correctly;
- an explicit profile selects `ProfileCredentialsProvider`, while an absent profile uses the default credentials
  provider chain;
- candidate selection includes only used, locally available, at-least-30-minute-old media with no pending OCR;
- both `recognizeAt == null` and `recognizedAt != null` are accepted, while scheduled/uncompleted OCR is excluded;
- claiming and completion use short transactions, and no database transaction or row lock is retained while the
  upload future is running;
- concurrent workers claim different rows with `FOR UPDATE SKIP LOCKED`;
- purge skips a row while its cloud-upload lease is set;
- heartbeat renewal compares and replaces the exact previous deadline;
- an expired lease gets a new deadline and recalculates the same deterministic object key;
- a stale worker cannot publish or clear a lease after its expected deadline changes;
- upload uses the persisted local filename and sets MIME type/content length;
- the storage wrapper uses a multipart-enabled async client for large files;
- a 150–200 MB upload is not read fully into heap and `cloudFileName` is published only after the completion future
  succeeds;
- the key uses `MediaFile.id`, `createdAt` Unix seconds, and `MimeUtil.extension`;
- a failed transfer attempts to abort incomplete multipart work, leaves `cloudFileName` null, and conditionally moves
  the lease deadline forward for retry;
- a crash after S3 completion but before publication recalculates and retries the same key without leaking a differently
  named object;
- a successful upload conditionally publishes `cloudFileName`, clears `cloudUploadDeadline`, and invalidates affected
  attachment caches without rebuilding them;
- the first subsequent request recreates an invalidated attachment cache with the S3 direct path;
- if usage drops to zero during upload, successful publication preserves the exact key for the subsequent removal
  tombstone;
- expired leases can be recovered after switching away from `AWSS3`, while missing S3 configuration leaves them
  safely leased and emits a warning;
- `AWSS3` returns no direct path before upload and an absolute presigned URL afterward;
- private media expose a distinct `directDownloadPath` with an attachment content disposition and a matching
  `directDownloadPathExpiresAt`;
- private download filenames use `MediaFileOwner.getUserFileName()` safely, including non-ASCII and quoted titles;
- public media, previews, and avatars do not expose download-direct fields and retain controller-path downloads;
- S3 URL duration comes from the supplied `ExtendedDuration`, preserves shorter lifetimes, and never exceeds seven
  days;
- a cached display URL refreshes both URL variants for the same validated key and rebuilds content disposition from
  media data rather than old query parameters;
- filesystem HMAC paths and expiration behavior remain unchanged;
- filesystem direct downloads are constructed server-side and retain the existing reverse-proxy behavior;
- Java and React URL helpers leave absolute URLs unchanged and prefix relative paths;
- explicit downloads use `directDownloadPath` unchanged and fall back to the controller path without mutating an S3
  signature;
- stable copied download links continue to use the controller path rather than an expiring presigned URL;
- cloud-only tombstones are deleted without acquiring the media-ID advisory lock;
- a recreated hash preserves its local file but does not preserve the old timestamped S3 object;
- S3 deletion failure retains the tombstone, while an already absent object removes it;
- the scheduled upload and removal workers do not overlap within one process.

Use a fake `AwsS3MediaStorage` for unit tests. Do not make live AWS calls in the normal test suite.

## Implementation Order

Split the work into two separately reviewable and mergeable parts.

### Part 1: Backend-Neutral Direct-Serving Refactor

This part must not add AWS dependencies, configuration, database state, or S3 behavior:

1. Introduce `DirectServeOperations`; preserve the existing `NONE` and `FILESYSTEM` behavior while adding distinct
   display/download direct-path creation and moving direct-path callers from static/configuration-based construction
   to the injectable service.
2. Add the private-media download fields to the API schema, expand the direct-path contract to allow relative or
   absolute locations, regenerate API artifacts, and update URL resolution and display/download selection in Java and
   React consumers. Do not add download-direct fields to public media, previews, or avatars.
3. Rebuild `../java-moeralib`, compile/test `moera-node`, test the handwritten React client, and run
   `git diff --check` in every repository touched by this part.

### Part 2: AWS S3 Support

Build the S3 implementation on the completed backend-neutral refactor:

1. Add `DirectServeSource.AWSS3`, the AWS SDK BOM and `s3` module, S3 configuration/validation, and the
   multipart-enabled SDK wrapper.
2. Add the durable upload-lease column/indexes and explicit claim, heartbeat, completion, and purge-exclusion
   repository queries.
3. Implement the leased scheduled uploader, deterministic naming, failure backoff, crash recovery, and attachment
   cache invalidation.
4. Add S3 display/download presigning, finite expiration, source-aware attachment-cache behavior, and cached-URL
   refresh.
5. Refactor tombstone cleanup so local deletion keeps its lock while exact S3 deletion is lock-free and retryable.
6. Update S3 documentation and configuration examples.
7. Compile/test `moera-node`, test the affected React flows, and run `git diff --check` in every repository touched by
   this part.
