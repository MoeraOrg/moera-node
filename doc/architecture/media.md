# Media

## Model and terminology

Media separates content identity from ownership and use.

`MediaFile` represents one byte sequence. Its primary key is the unpadded Base64URL form of a SHA-1 content hash, and
the row also stores a SHA-256 digest. Equal uploads reuse the same row and stored bytes. The record contains MIME type,
byte size, dimensions, orientation, video duration and stream data, OCR state, public exposure, storage locations, and
trigger-maintained usage state.

`MediaFileOwner` represents a private media item owned by one hosted node. It has a random UUID, title, permissions,
malware state, and a relation to `MediaFile`. Several owners, including owners from different hosted nodes, may share
one content row. `MediaFilePreview` links an original file to a preview, which is itself an ordinary `MediaFile`.

The public API deliberately hides this implementation split:

| Implementation term | Public API term |
| --- | --- |
| `MediaFileOwner.id` | media file ID |
| `MediaFile.id` | media file hash |

Public media is addressed by its content hash. Private media is addressed by its owner UUID. The extension in a public
or private controller URL is a browser hint; the controller parses the ID and reads the stored MIME type.

## Local and cloud copies

`MediaFile.fileName` is the exact basename of the local copy under `node.media.path`.
`MediaFile.cloudFileName` is the exact object key of a cloud copy. Each value is nullable and independently records
whether the application believes that copy is available. Both may be present, and a referenced metadata row can remain
when neither copy is available.

A storage name is chosen when a copy is created and is never reconstructed later from the content hash and current
MIME-to-extension rules. This keeps existing files reachable when extension mappings change. Virtual public and
private controller paths remain ID-based and are generated separately from physical storage names.

`MediaOperations.getPath()` resolves only a persisted local name. `openContent()` prefers the local copy. If only an S3
copy exists and S3 is configured, it downloads the object to a temporary file and deletes that temporary copy when the
returned `MediaFileContent` is closed. If neither copy can be opened, it throws `MediaFileNotAvailableException`.

The media root contains:

```text
<node.media.path>/<MediaFile.fileName>   persistent local content
<node.media.path>/tmp/                  temporary transfers and transforms
<node.media.path>/uploads/              resumable upload staging
```

`MediaManager` requires the configured media root to exist as a writable directory. At startup it creates the `tmp/`
and `uploads/` subdirectories when they are absent.

## Ingest and deduplication

Incoming content is streamed through `DigestingOutputStream` into a temporary file. The stream computes the SHA-1 ID
and SHA-256 digest while enforcing the configured size limit. `MediaOperations.putInPlace()` then:

1. takes a PostgreSQL transaction-level advisory lock namespaced as `media-file:<hash>`;
2. reuses an existing `MediaFile` on a deduplication hit and discards the new temporary copy;
3. detects the final content type for new content;
4. chooses a local basename once and moves the bytes into the media root;
5. probes image or video metadata; and
6. inserts the row with the exact local name and exposure state.

The file is moved before its name is committed to the database. A crash can therefore leave an orphan, but cannot
publish a name for a file that was never created. Filesystem and database changes are not a single atomic resource.

Public uploads accept supported images and set `exposed`. Private uploads create an owner and may accept the request
body, a completed staged upload, or a source URL. `downsize` keeps image resizing synchronous and requests asynchronous
compression for a video that does not meet the node's compressed-video profile.

## Resumable uploads

The admin-only API under `/moera/api/media/upload` stages a private source file without creating `MediaFile` or
`MediaFileOwner` records:

| Method | Behavior |
| --- | --- |
| `POST /media/upload` | create metadata and an empty sparse staging file |
| `GET /media/upload/{id}` | return progress and completion state |
| `PUT /media/upload/{id}/{chunk}` | write one zero-based chunk |
| `DELETE /media/upload/{id}` | delete the metadata and staged file |

The client declares the full size and may propose a chunk size. The server caps it at the per-node
`media.upload.max-chunk-size`; `media.max-size` caps the whole file. The current defaults are 8 MiB per chunk and
150 MiB per media item.

`media_uploads.uploaded_chunks` is a sorted PostgreSQL integer array. Chunks may arrive out of order. A process-local
lock serializes writes for one upload, and every non-final chunk must have the chosen size. The last chunk must exactly
fill the declared file size. The PUT that completes the set stores `completed_at`; later PUTs return the completed state
without rewriting the file.

A later `POST /media/private?upload=<id>` consumes a completed upload through the normal ingest path. Staged uploads
expire six hours after creation. Every three hours, cleanup removes expired pending or completed rows and their files.

## Previews

Image and video previews are content-addressed media files linked through `MediaFilePreview`. Image input supplies
dimensions and EXIF orientation. Very wide or tall originals may be cropped before preview scaling. Video previews are
JPEG frames created with `ffmpeg`.

When a client requests a width, the node chooses the smallest preview that is at least that wide and falls back to the
original when no suitable preview exists. Each preview has its own storage names and direct path; a preview never uses
the original file's physical name.

## Controller serving

Controller-backed public routes include:

```text
/moera/api/media/public/{hash}/data
/moera/media/public/{hash}.{extension}
```

The controller returns public content only when `MediaFile.exposed` is true.

Private routes include:

```text
/moera/api/media/private/{owner-id}/data
/moera/media/private/{owner-id}.{extension}
```

Private access is allowed when the owner is unrestricted, the request has the required local scope, or a valid media
grant is supplied. Malware restrictions are checked before serving. A remote grant is also checked against a
`MediaLease`. Unauthorized private media appears as not found.

A grant is a signed, expiring description of permitted private-media access. It may request a preview width or browser
download and carry a user filename. The normal grant and private direct-path lifetime is three days rounded up to the
next UTC midnight.

`node.media.serve` chooses the application delivery mechanism:

- `stream` streams bytes through Spring;
- `sendfile` returns `X-SendFile` for a supporting web server; and
- `accel` returns `X-Accel-Redirect` for nginx, using `node.media.accel-prefix`.

Authorization, preview choice, content disposition, MIME type, and cache policy stay in the application in all three
modes. Only the final transfer mechanism changes.

## Direct serving

Direct serving allows the reverse proxy or S3 to transfer bytes after the API has authorized the request.
`node.media.direct-serve.source` selects `none`, `filesystem`, or `aws-s3`.

Filesystem direct paths are relative to `/moera/media/` and contain the persisted basename plus `exp`, optional `fn`,
and `sig` parameters. The signature is HMAC-SHA-256 over the media hash, expiration, and optional filename, using the
shared `node.media.direct-serve.secret`. A download path also carries the download flag expected by the reverse proxy.

S3 direct paths are absolute HTTPS presigned URLs. The requested lifetime is capped at seven days and may be shortened
by the lifetime of temporary AWS credentials. A private media response can contain separate display and download URLs;
the download URL signs an attachment `Content-Disposition` header. Clients fall back to the controller path when a
direct URL is absent.

`DirectServeOperations` is the single backend-aware path builder. Server code resolves relative filesystem paths under
`/moera/media/` and leaves absolute S3 URLs unchanged. Cached attachment paths are refreshed without trusting their old
signing parameters. For S3, refresh accepts only an HTTPS URL in the configured bucket whose flat object key matches
the requested media hash and timestamped-key format.

## AWS S3 lifecycle

The node expects a private bucket provisioned outside the application. Configuration supplies bucket, region, and an
optional shared-configuration profile. With no profile, the AWS SDK default credentials provider chain is used. The
node needs these object permissions for the configured bucket or prefix:

- `s3:PutObject`;
- `s3:GetObject`;
- `s3:DeleteObject`; and
- `s3:AbortMultipartUpload`.

A bucket lifecycle rule should abort incomplete multipart uploads after a bounded period, such as seven days.

An S3 uploader runs every five minutes and processes at most 250 files. A new candidate must have a local copy, a
positive usage count, an age of at least 30 minutes, a completed SHA-256 digest upgrade, no cloud copy, and no active
video-compression job. OCR state does not affect eligibility.

Claiming uses a short `FOR UPDATE SKIP LOCKED` transaction and stores a 15-minute `cloud_upload_deadline`. The deadline
acts as a fencing token. While the asynchronous multipart-capable SDK transfers the file outside a transaction, the
worker renews its lease every five minutes with an exact compare-and-set update.

The deterministic object key is:

```text
<media-hash>_<created-at-as-Unix-seconds>.<MIME-extension>
```

After upload, a short conditional transaction publishes that exact key, clears attachment caches, and clears the
lease. Non-exposed media also lose their local copy and `fileName`; exposed public media retain both. A stale worker
whose deadline no longer matches cannot publish. An expired lease retries the same key, so crashes before database
publication do not create differently named objects.

Starting video compression prevents S3 claiming of the source. S3 publication also checks that no compression job has
appeared since the claim. This keeps a file locally available while `ffmpeg` still needs it.

## Video probing and compression

Every newly stored supported video is probed once with:

```text
ffprobe -v error -show_streams -show_format -of json <input>
```

The node stores normalized probe JSON in `MediaFile.streamInfo`, along with displayed dimensions, duration, and an
`uncompressed` classification. A deduplication hit preserves the existing row and does not probe again.

A video is treated as already compressed when it is MP4 with exactly one non-cover video stream using H.264 and
`yuv420p`, fits within an orientation-aware 1280x720 or 720x1280 box, and runs at no more than 30 fps. It may have at
most one AAC audio stream with one or two channels and a reported bitrate no higher than 144 kb/s. An absent audio
bitrate is accepted. A data stream makes the file uncompressed; subtitles and attached pictures do not by themselves.

For `downsize=true`, a private owner records that compression was requested. If the shared content is uncompressed,
the upload transaction takes a pessimistic media-row lock and either reuses an existing compressed result, joins the
existing job, or creates one global `VideoCompressionJob` with `Jobs.runAfterCommit()`. Owners cannot be attached to a
posting or comment while their requested conversion is pending, although drafts and media leases may retain them.

The job allows one initial attempt and five retries at 30-minute intervals, with at most two compression jobs running
in parallel. It reads local or cloud-only input through `openContent()` and uses an `ffmpeg` profile with:

- MP4, H.264 through `libx264`, CRF 23, `medium`, and `yuv420p`;
- orientation-aware downscaling to 1280x720 or 720x1280 without upscaling;
- a maximum of 30 fps;
- at most one AAC audio stream encoded at 128 kb/s, with larger layouts downmixed to stereo;
- best-effort subtitles and attached pictures, no data streams; and
- `+faststart` output.

Machine-readable progress is expected every five seconds. A five-minute absence of progress stops the process. The
overall deadline is `15 minutes + 15 * source duration`. Failed ancillary-stream preservation is retried with fewer
ancillary streams before the job attempt fails.

Before work and again before publication, the job locks the original row and verifies its own job ID and the absence
of a prior result. Publication creates one replacement owner for every `downsize` owner still referring to the
original, grouped under the correct hosted-node context. It copies title and permission state, links
`compressedOwner`, stores `compressedFile` on the source, and sends `MEDIA_COMPRESSED` client events after commit. The
original owner IDs and any drafts that reference them remain unchanged.

## OCR and derived text

`OcrJob` submits eligible media to OCR.Space. When the selected direct backend can provide an absolute URL, OCR.Space
receives that URL; otherwise the node uploads the local bytes. Recognition state and non-empty text are stored on the
shared `MediaFile`.

`OcrOperations` applies recognized text to every linked owner and entry revision. It clears serialized attachment
caches, recomputes headings and descriptions through the common text converter, emits posting or comment media-text
liberins, emits heading changes only when derived text changed, and emits draft changes for local media.

The public API calls the stored value `textContent`; persistence calls it `recognizedText`. For remote media,
`RemoteMediaFile` stores recognized text received from server-generated metadata or a lease notification. Client-sent
`RemoteMedia` cannot supply or overwrite OCR text.

When both local and remote representations are attached, heading extraction uses the local `MediaFile`. Remote OCR
text is a fallback only while no local file is attached. A downloaded remote attachment reads text from the actual
persisted or deduplicated local file and reapplies derived-text updates, which handles reuse of a file already
recognized on this node.

Non-empty local OCR results are propagated to lease holders as described in the federation chapter. Empty results mark
local recognition complete but do not clear previously propagated remote text.

## Remote download transaction boundary

Remote media downloads deliberately separate network and storage preparation from the final entry write transaction.
`MediaManager.preparePrivateMedia()` runs without an enclosing transaction. Short read transactions check an existing
owner and remote cache before taking the media-hash lock, so a matching committed result returns immediately. Remaining
work is serialized; after acquiring the lock, the method repeats both checks before downloading. Missing bytes are
downloaded, verified, and published through the independently committing content and cache operations. The method
returns scalar prepared data.

`MediaManager.ownPreparedPrivateMedia()` runs inside the caller's final write transaction. It resolves the owner again
or creates one from the verified local file and never contacts the remote node. A concurrent download of the same hash
does not prevent reuse of a committed owner or file. If the prepared file has disappeared, the method asks the caller
to retry rather than downloading while holding the write transaction.

`Picker` snapshots the posting and remote attachment keys before preparation. Its final transaction locks and reloads
the posting, rejects a stale snapshot, rereads attachments that other jobs may have added, and commits the posting,
revision, owners, attachments, permissions, reaction totals, and sources together. Missing supplied parents also cause
a retry. Liberins and child picks are dispatched after commit.

`RemoteMediaDownloadJob` creates or reuses the owner and builds its `PrivateMediaFileInfo` inside the same final
transaction so lazy preview relations are available. A failed final transaction may leave verified content in the
remote cache, but it cannot leave a partially updated posting or a new attached owner.

Remote lease requests are external effects and cannot be rolled back with the local database transaction. Older media
download callers and local preview creation retain their own transaction boundaries.

## Media child postings

When media is attached to an entry, the node may create a child posting for media-specific interaction. The child
stores both the media relation and the exact parent entry. The same owner may therefore have distinct child postings in
different parent-entry contexts.

Queries for current attached children join the parent entry's current revision and attachment to a child whose
`parentMediaEntry` is that parent and whose media relation matches the attachment. Permission propagation uses the same
direct relation. Removing an attachment from every revision of its parent causes normal posting deletion; an old
revision can keep the structural link alive even when the current attachment listing no longer shows it.

Remote and picker workflows carry parent-entry and remote-media state until the child can be created. Parent entry IDs
remain local structural metadata, while federated fingerprints use media digests and remote posting/media identifiers.

## Reference counting and cleanup

Database triggers update `MediaFile.usageCount` for owners, previews, avatars, attachments, compression results, and
other references. A zero count sets a four-day deadline; a new reference clears it. Owner usage uses the same deferred
pattern.

Every six hours, `MediaCleanupOperations.purgeUnused()` deletes expired unused owners and moves expired unused content
rows to `media_file_removals` in batches. The transaction copies the exact local and cloud names into a tombstone and
deletes the `media_files` row. It does no external storage I/O.

Every 30 minutes, a process-local single-run worker reads up to 100 tombstones. For a local name, it takes the same
media-hash advisory lock as ingest and checks whether the hash was recreated; a recreated row protects its local file.
Otherwise it deletes the recorded local path. Outside that lock and transaction, it deletes the exact recorded S3 key.
The tombstone is removed only after all applicable copies are gone. Missing files are success, and any failure retains
the tombstone for retry.

Local recreation is hash-based because the local basename may be reused. S3 deletion is exact-key based and still
removes the old timestamped object when the same content hash has been recreated. Cloud tombstones remain queued while
the active direct-serving source is not `aws-s3`.

## Deployment configuration

| Property | Purpose |
| --- | --- |
| `node.media.path` | root for persistent, temporary, and staged local media |
| `node.media.serve` | controller delivery: `stream`, `sendfile`, or `accel` |
| `node.media.accel-prefix` | nginx internal prefix for local media |
| `node.media.cloud-accel-prefix` | nginx internal prefix for cloud-backed controller delivery |
| `node.media.direct-serve.source` | direct backend: `none`, `filesystem`, or `aws-s3` |
| `node.media.direct-serve.secret` | shared HMAC secret for filesystem direct paths |
| `node.media.direct-serve.bucket` | private S3 bucket |
| `node.media.direct-serve.region` | S3 region |
| `node.media.direct-serve.profile` | optional AWS shared-configuration profile |
| `node.media.ocr-service` | configured OCR provider |
| `node.media.ocr-service-key` | OCR provider credential |

The runtime image must provide `ffprobe` and `ffmpeg` with H.264 encoding through `libx264` and AAC encoding support.
