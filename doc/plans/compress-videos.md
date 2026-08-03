# Compress videos on request

## Goal

Compress uploaded private videos asynchronously to a maximum 720p presentation when the caller passes `downsize=true`
to `POST /media/private`: landscape video fits within 1280x720, and portrait video fits within 720x1280. The upload
itself returns immediately with the newly created private-media owner, which may still reference the original file.
Until compression succeeds, the API marks that media as uncompressed and does not allow it to be attached to a post or
comment. Drafts may keep such media while compression is pending, and the media may be leased.

The output is an MP4 (`video/mp4`) suitable for browser playback. It has one primary H.264 video stream, zero or one
AAC audio stream, and may also contain subtitle and attached-picture streams preserved from the source. It does not
contain data streams.

## Definition of an already-compressed video

`MediaFile.uncompressed` means that the video needs the conversion described in this plan; it does not mean that the
source codec is literally uncompressed.

A newly stored video has `uncompressed=false` only when all of the following are true:

- the container/MIME type is MP4 (`video/mp4`);
- there is exactly one primary video stream, excluding streams with `disposition.attached_pic=1`;
- the primary video codec is H.264;
- its displayed dimensions, after applying rotation, fit within the box for its displayed orientation: 1280x720 when
  width is greater than or equal to height, or 720x1280 when height is greater than width;
- its pixel format is `yuv420p`;
- its reported frame rate is at most 30 fps; parse the rational frame-rate fields and behave conservatively if the
  rate is absent or invalid;
- there are zero or one audio streams; a silent video is valid;
- if an audio stream exists, it uses AAC, has at most two channels, and its reported bitrate is at most 128 kb/s; and
- there are no data streams.

Subtitle streams and attached-picture streams neither count as primary video streams nor force transcoding. Their
presence and codecs do not affect `uncompressed`. A reported audio bitrate greater than 128 kb/s forces transcoding; an
absent bitrate does not force transcoding by itself. Fast-start layout is guaranteed for files produced by this
feature, but is not used to classify an existing file because it is not represented reliably by the stored ffprobe
output.

Multiple primary video streams, multiple audio streams, a non-MP4 container, a nonconforming codec, resolution, pixel
format, channel count, or frame rate, audio bitrate above 128 kb/s, or any data stream sets `uncompressed=true`.

## Database changes

The base compression schema is added by `V299__compress_videos.sql`. Add `compressed_owner_id` in the follow-up
`V300__media_file_owner_compressed_owner.sql` migration so the already-committed V299 checksum stays unchanged. Do not
edit `doc/create_tables.sql`.

Add these columns to `media_files`:

- `stream_info text NULL` contains the exact JSON written to stdout by `ffprobe -show_streams -show_format`.
- `uncompressed boolean NOT NULL DEFAULT false` contains the classification above.
- `compressed_file_id varchar(40) NULL` references `media_files(id)` with `ON UPDATE CASCADE ON DELETE SET NULL`.
- `compression_job_id uuid NULL` references `pending_jobs(id)` with `ON UPDATE CASCADE ON DELETE SET NULL`.

Add these columns to `media_file_owners`:

- `downsize boolean NOT NULL DEFAULT false` records that the owner was created by a request asking for
  downsizing/compression. Replacement owners created by the compression job also have `downsize=true`.
- `compressed_owner_id uuid NULL` references `media_file_owners(id)` with `ON UPDATE CASCADE ON DELETE SET NULL`. It
  links an original owner to the replacement owner created from the compressed file.

Add indexes for all three new foreign keys. Add a trigger for `compressed_file_id`, following the existing media-file
reference triggers, so insert/update/delete of that reference adjusts the compressed file's `usage_count` exactly
once. Add the corresponding trigger for `compressed_owner_id`, using `update_media_file_owner_reference()`, so the
reference adjusts the replacement owner's `usage_count` exactly once and keeps it alive. `compression_job_id` is only
a lifecycle reference and must not affect media usage counts.

Map both foreign keys as optional, unidirectional JPA relationships on `MediaFile`: `MediaFile compressedFile` for
`compressed_file_id` and `PendingJob compressionJob` for `compression_job_id`. Use `@ManyToOne` without cascading; the
database foreign keys control deletion behavior. Map `compressed_owner_id` as the optional, unidirectional
`MediaFileOwner compressedOwner` JPA relationship on `MediaFileOwner`, also with `@ManyToOne` and no cascading. Map the
remaining columns in `MediaFile` and `MediaFileOwner`, and keep all entity setters/getters as plain value copies. All
repository operations must still have explicit queries.

Do not backfill or probe existing `media_files` rows. They keep `stream_info=NULL` and the migration default for
`uncompressed`, including when an old row is encountered through content deduplication. Only newly inserted video rows
are probed and classified.

## Probing and storing videos

Extend `VideoUtil` so one ffprobe invocation returns both the raw output and the parsed values needed by
`MediaOperations.putInPlace()`:

```text
ffprobe -v error -show_streams -show_format -of json <input>
```

Store stdout without normalizing or reserializing it. Parse the same string to obtain duration, stream types, codecs,
dimensions, rotation/display-matrix metadata, pixel format, frame-rate rationals, audio channels, dispositions, and
the classification result. Continue rejecting malformed videos with `media.video-invalid`.

For every newly inserted accepted video, including a file produced by the compression job:

- save the raw JSON in `stream_info`;
- populate the existing width, height, and duration fields;
- set `uncompressed` from the classification rules; and
- verify that a compression output classifies as compressed before publishing it.

Keep the existing ten-second ffprobe timeout. A deduplication hit returns the existing row without probing or changing
its `stream_info` or `uncompressed` value.

## Upload and job creation

For all three private-upload sources (request body, completed media upload, and URL), keep image downsizing synchronous
as it is today. After the media file and its owner are created, set `MediaFileOwner.downsize` to the request's
`downsize` value. If `downsize=false` or the stored file is not a video, do not start video compression.

When `downsize=true` for a video, lock the `media_files` row and perform the following decision in the upload
transaction:

1. If `uncompressed=false`, keep the new owner as-is.
2. If `compressed_file_id` is set, rebind this newly created owner to the compressed `MediaFile` and return that same
   owner ID to the client. This is safe because the owner ID has not left the request yet. Keep `downsize=true`.
3. If `compression_job_id` is set, keep the new owner on the original file; the existing job will process it.
4. Otherwise, persist one global video-compression job for the media file and set `compression_job_id` to that job's
   UUID in the same transaction.

All upload requests deciding the state of the same `MediaFile` must use the same row lock. This makes job creation
single-winner and serializes a late upload with the job's final owner scan. Avoid a new native query for this lock;
use an explicitly declared JPA repository query with a pessimistic write lock.

Keep the existing `Jobs.run()` API and its persistence, failure-handling, and scheduling semantics unchanged. Add a
separate `Jobs.runAfterCommit()` API so the caller can atomically persist the pending job, receive its UUID, assign
`compression_job_id`, and schedule execution only after commit. If the transaction rolls back, no task may be
scheduled. If persistent job creation fails, fail and roll back the upload instead of returning an owner that has no
job.

The job is global (`pending_jobs.node_id` is null), because `MediaFile` rows may be shared by owners from several local
nodes. Its parameters contain the original media-file ID. Make the current pending-job UUID available to the job for
the stale-job checks below.

## ffmpeg conversion

Read the original with `MediaOperations.openContent()` so compression also works if a source was already moved to S3
before compression was requested. Write each attempt to a temporary file and always remove that file after success,
failure, interruption, or timeout.

Build an ffmpeg command with this output profile:

- container and MIME type: MP4 / `video/mp4`;
- primary video encoder: `libx264`;
- quality: CRF 23;
- preset: `medium`;
- pixel format: `yuv420p`;
- dimensions: choose the bounding box from the displayed orientation after applying rotation, then fit landscape video
  inside 1280x720 or portrait video inside 720x1280 while preserving aspect ratio, with no upscaling;
- frame rate: preserve rates up to 30 fps and cap higher rates at 30 fps;
- audio: select at most one stream (prefer the default stream, otherwise the first), encode with AAC at 128 kb/s, and
  preserve mono/stereo while downmixing larger layouts to stereo;
- output layout: `-movflags +faststart`;
- map global metadata, chapters, stream metadata, and displayed rotation without double-applying rotation; and
- omit all data streams.

Select one primary video stream, preferring the default stream and otherwise the first non-attached-picture stream.
Preserve subtitle and attached-picture streams on a best-effort basis. Map/copy streams already compatible with MP4
and convert compatible text subtitles or attached pictures when needed. If MP4 cannot represent an ancillary stream,
omit that stream and log the decision; failure to preserve an ancillary stream must not prevent production of the
primary video and optional audio.

Use ffmpeg's machine-readable progress output:

```text
-progress pipe:1 -stats_period 5
```

Read stdout and stderr concurrently so neither pipe can block the process. Treat each valid progress report as a
heartbeat. Terminate ffmpeg when either:

- no progress report has arrived for five minutes; or
- wall-clock execution exceeds `15 minutes + 15 * source duration`, where duration and the multiplier are measured in
  seconds (maximum seconds = `900 + 15 * durationSeconds`).

On termination, request normal process destruction, wait briefly, and then destroy it forcibly if necessary. Preserve
bounded stderr for diagnostics and distinguish exit failure, no-progress timeout, wall-clock timeout, and
interruption in logs. Extend `ToolRunner` with progress/watchdog support without changing the existing ffprobe and
thumbnail callers unnecessarily.

After ffmpeg exits successfully, run the normal `putInPlace()` path on the output. This computes its hash and digest,
inserts or deduplicates the `MediaFile`, probes newly inserted output, stores `stream_info`, and creates normal video
previews. Reject the attempt if the resulting media file does not satisfy the already-compressed classification.

## Compression job lifecycle and retries

Configure the job for one initial attempt plus at most five retries, with a fixed 30-minute delay between attempts.
The existing `JobRetryCountPolicy` compares the incremented retry counter to an exclusive maximum, so use/configure it
carefully to produce five actual retries rather than five total executions. ffmpeg exit failures, watchdog timeouts,
output validation failures, storage failures, and transactional publication failures are retryable. Interruption must
restore the thread's interrupted status and use the normal job retry path.

At the beginning of every attempt, load and lock the original `MediaFile`. Finish the job immediately without running
ffmpeg if:

- `compressed_file_id` is already set; or
- `compression_job_id` is different from this job's UUID, including null.

Recheck the same conditions under a write lock before publishing the result. A stale job must never create replacement
owners or overwrite a newer job's result.

When all attempts fail, let the job finish as failed. Deleting its `pending_jobs` row automatically clears
`media_files.compression_job_id` through the foreign key. Do not send a failure event and do not add a separate failure
marker. A later upload of the same content with `downsize=true` can create a fresh job.

## Publishing the result and replacing owners

Once a valid compressed `MediaFile` exists, publish it in one database transaction:

1. Lock and revalidate the original `MediaFile` and job UUID.
2. Load every `MediaFileOwner` that still references the original and has `downsize=true`.
3. Group those owners by `node_id`. For each group, associate `UniversalContext` with that node before using node-aware
   media, grant, and event operations.
4. For every original owner, call the standard `MediaOperations.own(compressedFile, originalTitle)` method so normal
   previews and malware checks are used. Set the replacement's `downsize=true` and copy the original `unrestricted`
   permission state and permission timestamp. After saving the replacement, set the original owner's
   `compressedOwner` relationship to it. Do not copy usage counts, deadlines, postings, attachments, or leases.
5. Build `PrivateMediaFileInfo` for each replacement in its node context and queue a `MediaCompressedLiberin` containing
   the original owner ID and replacement info.
6. Assign `originalMediaFile.compressed_file_id` only after every replacement owner has been created successfully.

Keep owner creation, the final `compressed_file_id` assignment, and queued success liberins in the same transaction.
`UniversalContext` holds liberins until commit, so a rollback sends no partial events. Setting `compressed_file_id`
last also means the job's early-success check cannot skip unfinished owner work. Preview media rows committed by nested
media operations may become unused after a rollback; normal unused-media cleanup may remove them.

Each `compressed_owner_id` reference increments the replacement owner's `usage_count`, so the replacement remains
available as long as the original owner points to it. Clearing the relationship or deleting the original owner
decrements that count through the same trigger.

Do not modify, rebind, or delete original owners after their IDs have been returned to clients. Drafts that reference an
original owner remain unchanged; the client uses the event to replace that ID when appropriate. An owner created by a
concurrent upload is safe: the common `MediaFile` row lock makes it either part of the job's final scan or causes its
upload transaction to observe `compressed_file_id` and rebind it immediately.

After the publication transaction commits, let the job succeed. Removal of the pending job clears
`compression_job_id`; `compressed_file_id` remains the durable success signal and keeps the compressed media file in
use through its trigger-maintained reference count.

## Success event and public API

Add `MEDIA_COMPRESSED` to `EventType` and add a `MediaCompressedEvent` permitted to admin subscribers with
`Scope.VIEW_CONTENT`. Its payload is:

- `originalMediaId`: ID of the original `MediaFileOwner`; and
- `media`: `PrivateMediaFileInfo` for the replacement owner.

Add the corresponding liberin and receptor rather than sending directly from the job. The liberin carries the affected
node ID through `UniversalContext`, so owners of a shared media file receive separate events in their own local node
contexts. Only a success event is required.

Update `../moeraorg.github.io/_data/node_api.yml`, the source of truth for the public node API:

- add optional boolean `uncompressed` to `PrivateMediaFileInfo`, described as true while a requested video conversion
  is still required;
- describe optional `compressedMediaId` as the ID of the compressed media file, rather than its hash;
- change the `downsize` query-parameter description for `POST /media/private` to cover image downsizing and asynchronous
  video compression; and
- add `media.video-not-compressed` to the documented errors for create/update posting and create/update comment.

`PrivateMediaFileInfoUtil` sets `uncompressed=true` only when both `MediaFile.uncompressed` and
`MediaFileOwner.downsize` are true. If either value is false, it leaves the optional API field null/omitted.
It sets `compressedMediaId` from `MediaFileOwner.compressedOwner.id` when the replacement owner exists.

Add this message, next to the other `media.*` codes and following the ordering rules in `messages.properties`:

```properties
media.video-not-compressed=Video file compression is not complete
```

The error has no arguments or additional payload.

Run `../update-api` once after editing the schema, rebuild `../java-moeralib`, and compile the node. Review generated and
handwritten consumers in sibling repositories. In particular, add `MediaCompressedEvent` to the React client's event
types, schema, validator mapping, action union, and upload/editor state handling so it can replace the original owner
ID with the returned media info. Do not hand-edit generated node API artifacts.

## Blocking posts and comments

Add a shared predicate for a private owner that is waiting for video compression:

```text
MediaFileOwner.downsize == true
and MediaFile.uncompressed == true
```

Use it in `MediaOperations.validateAttachments()` for:

- `POST /postings`;
- `PUT /postings/{id}`;
- `POST /postings/{postingId}/comments`; and
- `PUT /postings/{postingId}/comments/{commentId}`.

Reject the whole operation with `OperationFailure("media.video-not-compressed")` if any requested local attachment
matches. Keep the draft controller's validation call explicitly permissive so creating and updating drafts remains
allowed.

Do not apply this check to `POST /media/leases`; media awaiting compression may be leased, including through
admin/draft-only leases.

## S3 coordination

The original file must not be published to S3 or have its local copy removed while `compression_job_id` is non-null.
Modify the existing native S3 claim query rather than adding a second native query:

- `claimCloudUpload()` must exclude rows with `compression_job_id IS NOT NULL`.
- Starting a compression job must revoke any outstanding cloud-upload claim on the original row by clearing its
  `cloud_upload_deadline` while holding the media row lock.
- `completeCloudUpload()` must require `compression_job_id IS NULL` as well as the expected upload lease. An uploader
  that lost the race may finish transferring bytes, but it must not publish the S3 name or delete the local file.

Once the pending job row is removed and the foreign key clears `compression_job_id`, the normal S3 scheduler may claim
the original again. Compressed output follows the ordinary S3 lifecycle.

## Tests

Add focused tests for:

- parsing and retaining raw ffprobe JSON;
- classification of H.264/AAC MP4, silent video, subtitles, attached pictures, data streams, multiple primary video or
  audio streams, audio bitrate below, at, and above 128 kb/s, absent audio bitrate, rotation, and both 1280x720 landscape
  and 720x1280 portrait bounds, including square video, pixel format, channel count, and rational frame rates;
- malformed ffprobe output and the existing probe timeout;
- ffmpeg command construction, orientation-dependent 1280x720/720x1280 no-upscale behavior, 30-fps cap, stream
  selection, data-stream removal, metadata/rotation mapping, and best-effort ancillary streams;
- progress heartbeat parsing, five-minute stall detection, the dynamic wall-clock deadline, graceful/forced process
  termination, interruption, and bounded diagnostics (make timeout values injectable in tests rather than waiting in
  real time);
- migration foreign keys, `compressed_file_id` and `compressed_owner_id` usage-count trigger behavior, and pending-job
  deletion clearing `compression_job_id`;
- atomic single-job creation under concurrent uploads and scheduling only after commit;
- immediate owner rebinding when a compressed result already exists;
- stale-job exits before and after ffmpeg;
- one initial attempt plus five 30-minute retries and automatic cleanup after final failure;
- all-or-nothing replacement-owner creation, copied title/permissions, `downsize=true`, normal previews/malware marks,
  and one success event per original owner in the correct node context;
- an owner arriving concurrently with publication;
- optional `PrivateMediaFileInfo.uncompressed` serialization for all combinations of `MediaFile.uncompressed` and
  `MediaFileOwner.downsize`;
- `PrivateMediaFileInfo.compressedMediaId` serialization from the replacement owner ID;
- rejection of posting/comment creation and edits with `media.video-not-compressed`, while draft creation/update and
  media lease creation succeed; and
- S3 claim exclusion, revocation of an in-flight claim, and resumption after the job row is removed.

Run the API regeneration/build workflow, relevant unit and integration tests, the full node test suite, and
`git diff --check` in every touched repository. The runtime/deployment image must provide ffprobe and an ffmpeg build
with `libx264` and AAC encoding support.

## Query guidance

Continue the repository rule that every JPA repository query is explicit. Reuse JPQL/pessimistic locking for media and
owner lookups. The only native-query changes expected for this feature are modifications to the existing S3 claim
query and the migration functions/triggers required for reference counting; avoid adding native queries elsewhere.
