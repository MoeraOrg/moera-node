# Persistent Media Storage Names Plan

## Goal

Stop deriving the name of a stored media object every time from `MediaFile.id` and `MediaFile.mimeType`. The extension
mapping is an implementation detail of `MimeUtil` and may change after a file has already been written.

Store the actual names of the filesystem and cloud copies in `media_files` instead. Each name is nullable and therefore
also records whether that storage copy is known to exist.

The content hash remains the `MediaFile` identity. Storage names are locations of copies of that content, not new media
identifiers.

## Naming And Invariants

Add these nullable fields to `MediaFile`:

- `fileName` / `media_files.file_name`: basename of the copy in the configured local media directory.
- `cloudFileName` / `media_files.cloud_file_name`: object name/key of the cloud copy.

Use `varchar(50)` for `file_name` and `varchar(65)` for `cloud_file_name`. The local name consists only of the current
media identifier, a dot, and an extension; the cloud name is at most 15 characters longer. A cloud object name may
contain a storage-specific prefix, while `file_name` must always be a single basename and must never escape the
configured media directory.

The fields have the following invariants:

- `file_name IS NOT NULL` means that the application believes a local copy exists under exactly that name.
- `cloud_file_name IS NOT NULL` means that the application believes a cloud object exists under exactly that key.
- `NULL` means that the corresponding copy is absent or its location is unknown.
- Both fields may be non-null at the same time. This supports migration between backends and redundant copies.
- Both fields may be null while the metadata row is still referenced. The media is then unavailable, but the database
  relations and metadata remain valid.
- A non-null storage name is never reconstructed from `id` and `mimeType`. It changes only when a copy is created,
  moved, or deleted.
- `MimeUtil.fileName(id, mimeType)` is used only once when choosing a name for a newly created storage copy. Later
  changes in `MimeUtil` do not change that copy's name.

Setters and getters on `MediaFile` should only copy values. Name generation, validation, storage access, and existence
checks belong in the media/storage operations layer.

## Virtual Names Are Unchanged

Do not persist or otherwise change names constructed from `MediaFileOwner`. They are virtual controller names. The
controller parses the owner ID from these names, and the extension is only a browser hint.

The same distinction applies to the non-direct public path. `/moera/media/public/{id}.{ext}` is a virtual route whose
controller looks up `MediaFile.id`; it is not a filesystem location. Continue generating its display extension from the
MIME type.

In particular:

- Keep `MediaFileOwner.getFileName()` and `getUserFileName()` behavior unchanged.
- Keep private paths based on the owner ID unchanged.
- Keep remote-media virtual paths unchanged.
- Change `MediaUtil.publicPath(MediaFile)` so that it explicitly builds the virtual `id.ext` name instead of reusing a
  physical-storage getter.
- Avoid a generic `MediaFile.getFileName()` whose meaning could be confused between a virtual name and a local storage
  name. Prefer explicit `getFileName()` for the persisted field only and explicitly named helpers for virtual names and
  storage paths.

No node API fields or fingerprints need to change. `path` remains a virtual controller path, and `directPath` contains
the selected storage name as it does today.

## Database Migration And Existing Files

Add a Flyway migration that adds the two nullable columns. Do not edit `doc/create_tables.sql`.

Do not backfill `file_name` in SQL by evaluating `id + extension(mime_type)`. PostgreSQL does not have the exact Tika and
`MimeUtil.ADDITIONAL_MIME_TYPES` mapping, and recomputing the name is precisely the behavior this change must remove.

Add an idempotent upgrade job that reconciles existing rows with the actual media directory:

1. Process `MediaFile` rows whose `file_name` is null in bounded batches.
2. For each upgrade record in the current batch, scan only for regular files whose basename starts with that exact media
   ID followed by `.`. Do not load all media IDs or list/index the complete media directory.
3. The old `MimeUtil.fileName(id, mimeType)` value may be checked as a fast path, but it must not be the only lookup.
4. Store the basename that is actually present. Do not rename the file during this backfill.
5. If no matching file exists, leave `file_name` null and count/log it as a missing local copy.
6. If several candidates exist, do not recalculate their content hashes. Leave the field null and report the ambiguity.
7. Leave `cloud_file_name` null for all existing rows; there is no existing cloud copy to backfill.

The application must tolerate null names while the upgrade job is running. The release that introduces these columns
must complete this disk-based backfill before any change to the legacy MIME-to-extension mapping is deployed. The job is
restartable: updating a row only after a candidate has been found makes a crash safe, and already populated rows do not
need to be revisited.

Report summary counts for populated, missing, and ambiguous records.

## Creating A Local Copy

Refactor `MediaOperations.putInPlace(...)` around the persisted local name:

1. Detect the final content type before choosing the destination name.
2. For a new `MediaFile`, generate the local basename once with `MimeUtil.fileName(id, contentType)`.
3. Move the temporary file to that exact destination.
4. Set `MediaFile.fileName` to the same basename and persist it with the rest of the metadata.

If a `MediaFile` row with the same content hash already exists, preserve the current deduplication behavior: discard the
new temporary file and return the existing row. Do not create or restore a storage copy and do not change either stored
name. The invariant is that an existing `MediaFile` has a usable copy in the filesystem or cloud; upload/download paths
do not verify this assumption.

Filesystem and database updates are not one atomic transaction. Use this ordering:

- On creation, write or move the object first and publish its name in the database second. A crash may leave an orphan,
  but it must not leave a non-null DB name for an object that was never created.
- On deletion, delete the object first and clear the DB name second. A crash may leave a stale non-null name, but retrying
  deletion is idempotent and can then clear it. Never clear the only known name before attempting deletion.

Use a PostgreSQL transaction-level advisory lock keyed by the namespaced `media-file:` prefix and media ID around the
lookup and publication of a new `MediaFile`. Namespacing prevents collisions with advisory locks for other entity
types. The removal worker must take the same lock before checking for a recreated row and deleting storage objects.
This makes the check and external deletion safe across application instances and prevents an ABA race in which a new
copy is published under the same name after the worker's check.

## Storage Access

Replace all physical path construction with a helper such as `getLocalPath(MediaFile)` that resolves the persisted
`fileName` under `node.media.path`. It must not fall back to `MimeUtil.fileName(...)` when the field is null.

Update at least these local-file consumers:

- streaming, `X-Sendfile`, and `X-Accel-Redirect` serving in `MediaOperations`;
- digest calculation;
- image reading, cropping, and preview generation;
- public-media upload to another node in `MediaManager`;
- OCR multipart upload;
- cleanup and upgrade jobs.

Operations that require a local copy must handle a null local name as unavailable instead of constructing a guessed
path. The media controllers should return the existing not-found/storage failure appropriate to the operation. They
should not clear a stored name merely because an access fails. A cloud-only file can be supported by those operations
later through a storage abstraction or a temporary local download; they must not accidentally read a legacy-derived
local path.

Low-level local-path resolution throws `MediaFileNotAvailableException`, an `IOException`, when `file_name` is null.
HTTP serving translates it to `ObjectNotFoundFailure`, node-to-node operations translate it to
`MoeraNodeLocalStorageException`, and background jobs log and skip media without a local copy.

`MediaFileRenamePaddedIdsJob` must rename a local file whose name starts with the padded ID and update `file_name`
accordingly. It must preserve the existing extension instead of deriving old and new paths from MIME types. Its
duplicate-row branch must explicitly preserve one usable copy and clean up or report the other copy.

## Direct Paths

Direct paths must select the location from the configured direct-serving backend:

- `NONE`: no direct path.
- `FILESYSTEM`: use `MediaFile.fileName`; return no direct path when it is null.
- A future cloud direct-serving source: use `MediaFile.cloudFileName`; return no direct path when it is null.

Keep signing the URL with the content hash/ID as today unless the cloud serving design requires provider-native signed
URLs. The URL location and the signed identity have different roles: the location comes from the selected persisted
storage name, while the media ID remains the stable identity covered by the signature.

Refactor direct-path construction so callers pass `MediaFile` (or both persisted storage names) rather than rebuilding
the location from API DTO fields. This affects:

- `MediaUtil.directPath(MediaFileOwner, ...)`;
- `PublicMediaFileInfoUtil`;
- `PrivateMediaFileInfoUtil`;
- `MediaFilePreviewInfoUtil`;
- `AvatarImageUtil` and `AvatarInfoUtil`;
- local attachment and HTML image path generation.

When refreshing an attachment cache, do not load `MediaFile` rows again. Reuse the direct filename already present in
the cached `directPath` and replace only its expiration and signature; preserve the filename and all other query
parameters. Clear all existing attachment caches once after the local-name backfill completes.

Every preview is itself a `MediaFile`; its direct path must therefore use the preview file's own selected storage name,
not the original file's name. The virtual preview path continues to use the original owner ID and `width` parameter.

Cloud support should add its direct-serving configuration only when its URL shape and signing method are defined. The
database field and backend selection described here keep that addition independent from MIME extension mappings.

## Absence Semantics

Assume that filesystem and cloud objects do not disappear outside application-controlled operations. Do not add a
periodic existence scan, provider HEAD checks, or operation-time clearing of names after read failures; checking all
non-null rows would be too costly.

Set a storage name to null only as part of an application-controlled successful deletion or move of that copy. If an
unexpected storage access fails, return/log the appropriate error while retaining the stored name. The one-time upgrade
job is the only full filesystem inspection required by this change.

## Deletion And Cleanup

Add a `media_file_removals` tombstone table with a synthetic primary key, the removed media ID, `file_name`,
`cloud_file_name`, and creation time. Index the removed media ID.

Change `MediaOperations.purgeUnused()` to process bounded batches. In one database transaction it must lock eligible
unused rows, copy their IDs and storage names to `media_file_removals`, and delete them from `media_files`. Foreign-key
locking then prevents a concurrent reference from being committed against a row being moved. No external storage
operation is performed by this transaction.

Add a scheduled removal worker that runs every 30 minutes and processes tombstones in bounded batches. A process-local
atomic running flag must prevent a new scheduled invocation from starting while the previous one is still running. For
each tombstone, in a separate transaction:

1. Lock the tombstone and acquire the media-ID advisory lock also used by creation.
2. Recheck `media_files` for the same ID.
3. If the ID has been recreated, delete the tombstone without deleting any storage object. This deliberately prefers a
   possible orphan over deleting an active copy.
4. Otherwise delete the exact recorded local and cloud objects. Treat an already absent object as success.
5. Delete the tombstone only after every recorded copy was deleted successfully or was already absent.
6. On a transient deletion failure, retain the tombstone, log the error, and retry it during a later pass.

Multiple node instances may select the same tombstone; deletion is idempotent, and the media-ID advisory lock serializes
their existence checks and external deletions. Holding that lock across the check and deletion is mandatory; a plain
check followed by deletion has an ABA race with upload/download publication.

The tombstone makes cleanup restartable. A crash before deletion leaves the work queued; a crash after external
deletion leaves an idempotent retry; and a database rollback cannot lose the stored object names needed for retry.

The same storage helper should be used by any explicit future move, copy, or delete operation. Successful creation sets
the corresponding name; successful deletion clears it.

## Repository Changes

Add explicit JPA queries for the new maintenance operations, following the project repository rules:

- find rows needing initial local-name backfill;
- move bounded batches of still-unused rows into `media_file_removals`;
- lock and delete tombstones;
- acquire the transaction-level advisory lock by media ID;
- recheck whether a media ID was recreated.

Do not add `@Column` annotations for `fileName` or `cloudFileName`; the default snake-case conversion already maps them
to the intended columns.

## Tests

Add focused tests for:

- a new local copy persists the exact generated basename;
- changing or overriding a MIME extension mapping after creation does not change local reads, deletes, or direct paths;
- a deduplicated upload/download discards the new temporary file and does not change either storage name;
- no local path is guessed when `file_name` is null;
- filesystem direct paths use `file_name`, and are absent when it is null;
- cloud direct paths use `cloud_file_name`, once that direct-serving source is implemented;
- preview direct paths use the preview `MediaFile` storage name;
- owner-based private paths and public virtual controller paths retain their current ID-based behavior;
- the upgrade job records the basename actually found on disk even when it differs from the current MIME mapping;
- missing and duplicate files are reported without destructive cleanup;
- cleanup deletes the tombstone only after every recorded copy was deleted successfully or was already absent, and
  retains it on transient failures;
- a recreated media ID prevents deletion of the tombstone's files;
- publication and tombstone cleanup use the same media-ID lock, so recreation cannot race the final existence check;
- padded-ID migration renames the local file and updates its persisted name without deriving the extension from MIME.

## Implementation Order

1. Add the nullable database columns and `MediaFile` fields.
2. Introduce explicit virtual-name, local-path, and direct-location helpers; remove physical-path derivation from MIME
   type outside new-copy creation.
3. Update local creation, reads, serving, OCR, preview generation, and deletion while retaining existing deduplication.
4. Change direct-path builders to use the persisted name selected for the configured backend.
5. Add and run the idempotent filesystem backfill job before changing any MIME extension mapping.
6. Move unused media into tombstones transactionally and make external storage cleanup retryable.
7. Update the padded-ID upgrade path.
8. Add the tests above and verify stream, sendfile, accel, and direct-filesystem serving modes.
9. When cloud storage is introduced, set/clear `cloud_file_name` only after provider operations succeed and add the
   cloud direct-serving source without changing virtual controller names.
