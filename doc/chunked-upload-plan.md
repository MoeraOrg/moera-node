# Chunked Upload Plan

## Goal

Add a resumable chunked upload flow for private media source files. Completion is implicit: the PUT request that makes
the set of uploaded chunks complete marks the upload as complete.

Creating the private media file and media owner is not part of this upload process. A later private-media operation will
consume a completed upload and create the media objects.

All API paths below are relative to `/moera/api`. All endpoints are admin-only and should use the same private media
upload scope as `POST /media/private`.

## Endpoints

### POST /media/upload

Create a pending upload.

Request structure: `MediaUploadAttributes`.

Fields:

- `mimeType`: optional, same semantics as the `Content-Type` header of `POST /media/private`.
- `title`: optional media title.
- `fileSize`: required full file size in bytes.
- `chunkSize`: optional client-proposed chunk size.

The server chooses the actual chunk size as follows:

- If `chunkSize` is absent, use the new option `media.upload.max-chunk-size`.
- If `chunkSize` is present and not larger than `media.upload.max-chunk-size`, use the proposed value.
- If `chunkSize` is present and larger than `media.upload.max-chunk-size`, ignore the proposal and use
  `media.upload.max-chunk-size`.

Create a record in `media_uploads` with an empty `uploaded_chunks` array and a deadline 6 hours from creation. Create an
empty sparse file of `fileSize` bytes in the `uploads/` directory, using the upload ID as the filename.

Return `MediaUploadInfo` with the upload ID, selected chunk size, file metadata, sorted uploaded chunks (empty), and
deadline.

Need to check `fileSize` against `media.max-size`.

### GET /media/upload/{id}

Return `MediaUploadInfo` for the upload with the given ID.

The `uploadedChunks` field contains raw zero-based chunk numbers sorted in natural order. It does not return byte ranges
or byte counts.

If the upload is already completed but not yet expired or consumed, return the same upload metadata with `completedAt`
set.

### PUT /media/upload/{id}/{chunk}

Upload one chunk of the file with the given ID.

Chunk numbers are zero-based. The byte offset is `chunk * chunk_size`. Chunks may be uploaded out of order. Parallel
requests are allowed, but implementation should protect each upload with an upload-specific lock or equivalent
serialization while writing, updating chunk state, and marking completion.

Validation:

- `chunk` must be in the range `[0, total_chunks)`.
- Every non-last chunk must contain exactly `chunk_size` bytes.
- The last chunk must contain exactly `file_size - chunk * chunk_size` bytes.
- A chunk must never cross the declared `file_size`.

Write the request body to the sparse upload file at the calculated offset. Add the chunk number to `uploaded_chunks` if
it is not already present, and keep the array sorted.

Duplicate chunk uploads must not fail while the upload exists. Their consequences are intentionally undefined: clients
that upload the same chunk more than once take responsibility for any overwrite or race they create.

When `uploaded_chunks` contains all chunks:

1. Set `completed_at`.
2. Leave the file in the `uploads/` directory.
3. Return `MediaUploadInfo` with `completedAt` set.

Do not calculate the media hash or digest, do not validate image contents, and do not create `MediaFile` or
`MediaFileOwner` during upload completion. These happen later, when a completed upload is used.

After completion, further PUTs for this upload should not rewrite the completed upload file. They should return the
completed `MediaUploadInfo` while the upload row still exists.

### DELETE /media/upload/{id}

Delete the upload with the given ID.

Delete the row and the corresponding file from the `uploads/` directory, whether the upload is pending or completed.
This endpoint does not delete any private media objects that may have been created later from this upload.

## Data Model

Add `media_uploads`.

Columns:

- `id uuid NOT NULL PRIMARY KEY`
- `node_id uuid NOT NULL`
- `mime_type varchar`
- `title varchar`
- `file_size int NOT NULL`
- `chunk_size int NOT NULL`
- `uploaded_chunks int[] NOT NULL`
- `deadline timestamp without time zone NOT NULL`
- `completed_at timestamp without time zone`

Add an index for cleanup and lookup by node/deadline.

Do not create a separate `media_upload_chunks` table. Chunk state is stored only in the array-typed
`media_uploads.uploaded_chunks` field.

Add a JPA entity `MediaUpload` and repository with explicit queries. Do not add `@Column` annotations where the default
snake-case conversion already gives the correct column name.

## Options

Add option `media.upload.max-chunk-size`.

Suggested type and modifiers:

- Type: `int`
- Privileged: `true`
- Default: choose a practical value below `media.max-size`, for example 1048576.
- Format: `size`
- Minimum: at least 65536.
- Maximum: no larger than `media.max-size` maximum.

The option is both the server default chunk size and the upper bound for a client-proposed chunk size.

## Storage

Create an `uploads/` subdirectory under the configured media path in `MediaManager.init()`, alongside the existing
`tmp/` setup.

Pending upload files are named by upload ID and have no extension. Creating the upload should set the file length to the
declared `fileSize` so random chunk writes can target exact offsets.

Completion leaves the upload file in `uploads/`. The later operation that creates private media from the upload is
responsible for consuming the completed upload. Expired uploads that have not been consumed are removed by cleanup.

## API Types

All new structures should be named `MediaUpload*`.

Suggested structures:

- `MediaUploadAttributes`: creation request.
- `MediaUploadInfo`: returned by POST, GET, PUT, and DELETE when a body is needed.

`MediaUploadInfo` should include:

- `id`
- `mimeType`
- `title`
- `fileSize`
- `chunkSize`
- `uploadedChunks`
- `deadline`
- `completedAt`

Update `../moeraorg.github.io/_data/node_api.yml` first, then regenerate API artifacts with `../update-api` during
implementation.

## Validation And Errors

Reuse existing codes where they fit:

- `media.wrong-size` for full media size over the configured maximum.
- `media.title.wrong-size` for an oversized title.
- `media.storage-error` for filesystem/storage failures.

Add missing upload-specific codes to `messages.properties`, keeping the file's existing ordering rules:

- `media-upload.not-found`
- `media-upload.chunk.invalid`
- `media-upload.chunk.wrong-size`
- `media-upload.chunk-size.invalid`

Validate:

- `fileSize` is present and positive.
- selected `chunkSize` is positive.
- chunk number is numeric and in range.
- request body length matches the expected chunk length.
- upload belongs to the current node.

## Cleanup

Run a periodic task once every 3 hours.

For expired pending uploads, delete the upload file from `uploads/` and delete the row.

For expired completed uploads that have not been consumed, also delete the upload file from `uploads/` and delete the
row.

## Implementation Checklist

1. Update the API schema and regenerate API artifacts.
2. Add the DB migration for `media_uploads`; do not edit `doc/create_tables.sql`.
3. Add `MediaUpload`, `MediaUploadRepository`, and conversion helpers for `MediaUploadInfo`.
4. Add upload storage helpers in the media layer.
5. Add `MediaUploadController` endpoints.
6. Add the cleanup job.
7. Add options and validation messages.
8. Add tests for creation, out-of-order upload, duplicate upload, completion, completed retry, delete, expiration, and
   boundary chunk sizes.
