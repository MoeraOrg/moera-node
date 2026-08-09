# Propagate recognized media text to remote nodes

## Goal

Keep OCR text with cached remote media, use it when deriving entry headings/descriptions, and propagate later OCR results
to lease holders and search indexes.

The server-produced value is called `recognizedText` in persistence and `textContent` in the public API. Recognition
timestamps remain local to `MediaFile`; `RemoteMediaFile` does not need recognition scheduling or completion timestamps.

## Decisions

- Do not add `textContent` to `RemoteMedia`. That structure contains media metadata supplied by a client, and clients do
  not perform or report OCR.
- Add optional `textContent` to `RemoteMediaInfo`, which is built by the node from stored `RemoteMediaFile` data.
- Add a `LEASED_MEDIA_TEXT_UPDATED` notification. The receiver validates the exact lease synchronously and performs the
  linked-object work in a durable after-commit job.
- A received notification updates only `RemoteMediaFile` rows carrying the notification's exact lease ID. A node may
  receive and process the same OCR result once for each lease it holds.
- Extend the existing posting/comment media-text update pipeline with an optional remote media node name. A local media
  update leaves it absent; a remote media update identifies the attachment by remote node name and media ID.
- Do not backfill existing `remote_media_files` rows. They remain without recognized text until normal metadata refresh
  or a later OCR notification supplies it.
- When both local and remote representations are present, heading extraction uses the local `MediaFile` exclusively.
  `RemoteMediaFile.recognizedText` is a fallback only when the local `MediaFile` itself is absent.
- After downloading a remote attachment, use recognized text from the resulting persisted `MediaFile`, if present, to
  update the newly linked entries. Do not rely on the downloaded metadata alone, because media storage may reuse a
  `MediaFile` that already exists on the receiving node and already has OCR text.
- Propagate only non-empty OCR results. An empty result records OCR completion on the source `MediaFile`, but does not
  clear previously propagated remote text.

## Public protocol and generated artifacts

Edit the schema sources before changing generated classes:

1. In `../moeraorg.github.io/_data/node_api.yml`:
   - add optional `textContent` to `RemoteMediaInfo` with the same meaning as
     `PrivateMediaFileInfo.textContent`;
   - add optional `mediaNodeName` to `SearchPostingMediaTextUpdate` and
     `SearchCommentMediaTextUpdate`, using the existing node-name length constraint;
   - keep `mediaId` as the media ID on the node selected by `mediaNodeName`, or on the posting/comment node when
     `mediaNodeName` is absent.
2. In `../moeraorg.github.io/_data/notifications.yml`:
   - add `LEASED_MEDIA_TEXT_UPDATED` with the next notification ordinal, `mediaId`, inherited `leaseId`, and optional
     `textContent` (using the existing media text length constraint);
   - add optional `mediaNodeName` to `POSTING_MEDIA_TEXT_UPDATED` so posting subscribers can distinguish local
     media from media originating on another node.
3. Run `../update-api` once from `moera-node`, then rebuild `../java-moeralib` with `mvn clean install`.
4. Update handwritten consumers after generation. Do not hand-edit generated library or client API files and do not
   change snapshot versions.

## Persistence and conversion

1. Add migration `V303__remote_media_file_recognized_text.sql` with a nullable `text` column named `recognized_text` on
   `remote_media_files`. Do not edit `doc/create_tables.sql` and do not backfill the column.
2. Add the plain `recognizedText` field and logic-free getter/setter to `RemoteMediaFile`.
3. Add explicit repository queries needed to:
   - verify that a row exists for `(nodeId, senderNodeName, mediaId, leaseId)` before accepting a notification;
   - load/update only rows matching that exact tuple for the asynchronous job;
   - find revisions linked to the affected remote-media rows without broadening the update to other leases.
4. Copy the stored value to `RemoteMediaInfo.textContent` in `RemoteMediaInfoUtil.build`; the minimal form does not need
   it.
5. When remote media is created or refreshed from server-returned `PrivateMediaFileInfo`, copy its `textContent` to
   `RemoteMediaFile.recognizedText` and run the linked-object update if the value changed. In particular, include it in
   `RemoteMediaOperations.store`, `RemoteMediaOperations.update`, and the comparison performed by
   `DownloadEntryAttachmentsJob`.
6. Leave all conversions from client-supplied `RemoteMedia` unchanged with respect to text. When posting/comment revision
   code reuses an existing `RemoteMediaFile`, preserve its recognized text rather than replacing it from `RemoteMedia`.

## Shared OCR-linked object operations

Extract the linked-object portion of `OcrJob` into an injected `OcrOperations` component, with separate entry points for
local and remote media but one shared revision-update path.

For local media, preserve the current behavior:

- find every `MediaFileOwner` for the recognized `MediaFile` and associate its node context;
- clear affected attachment caches;
- recompute headings and descriptions for every linked revision through `TextConverter.headingToRevision`;
- deduplicate current entries and emit `PostingMediaTextUpdatedLiberin` or `CommentMediaTextUpdatedLiberin`;
- emit the existing heading-update liberins only when the derived heading or description changed;
- emit `DraftUpdatedLiberin` for linked drafts.

The local entry point must be reusable by both `OcrJob` and the attachment-download path. It updates derived entry data
and emits entry/media/heading liberins, but it does not itself imply that OCR has just completed and therefore does not
send lease OCR notifications. `OcrJob` remains responsible for sending `MediaRecognizedTextUpdatedLiberin` after new
OCR.

For remote media:

- operate only on revisions linked to the exact `RemoteMediaFile` rows selected by sender node, media ID, and lease ID;
- clear their attachment caches and recompute headings/descriptions through the same shared path;
- deduplicate current entries before emitting media-text and heading-update liberins;
- emit media-text liberins with the originating media ID and `mediaNodeName` set to the sender node;
- do not add draft processing, because drafts do not contain `RemoteMediaFile` attachments.

Extend `PostingMediaTextUpdatedLiberin` and `CommentMediaTextUpdatedLiberin` with optional `mediaNodeName`. Update
their receptors and notification builders so this identity reaches both posting subscribers and search subscribers.
When building `PostingMediaTextUpdatedNotification`, `SearchPostingMediaTextUpdate`, or
`SearchCommentMediaTextUpdate`, pass the same field name through unchanged. Local OCR and downloaded-local-media paths
pass `null`; remote OCR paths pass the origin node name.

## Source-node OCR notification

1. Add `MediaRecognizedTextUpdatedLiberin`, containing the media-file-owner ID and `textContent`.
2. After `OcrJob` stores a non-empty OCR result, delegate linked-object updates to `OcrOperations`. For each affected
   `MediaFileOwner`, also send `MediaRecognizedTextUpdatedLiberin` in that owner's node context.
3. Handle the liberin in `MediaReceptor` and send `LEASED_MEDIA_TEXT_UPDATED` through
   `Directions.leases(nodeId, mediaId)`. Add a notification builder analogous to
   `LeasedMediaTitleUpdatedNotificationUtil`; lease routing supplies the individual `leaseId`.
4. Retain the current behavior for null or empty OCR results: store the result/completion time locally, but skip linked
   updates and lease notifications.

## Receiving-node job

1. Add a `MediaProcessor` mapping for `LEASED_MEDIA_TEXT_UPDATED`.
2. Before accepting it, synchronously verify a `RemoteMediaFile` for the receiving node, sender node, media ID, and exact
   lease ID. If none exists, throw `ObjectNotFoundFailure("media-lease.not-found")`, matching title-update behavior.
3. Enqueue a persistent after-commit job with the receiving node ID and immutable parameters containing sender node name,
   media ID, lease ID, and text content.
4. In the job transaction, reload all rows matching that exact tuple, set `recognizedText`, and invoke the remote path of
   `OcrOperations`. Make retries idempotent: setting the same text and recomputing the same revisions must be safe.
5. If the lease disappears after synchronous validation but before job execution, finish as a no-op rather than updating
   rows belonging to another lease.

## Downloaded attachment with existing OCR text

`DownloadEntryAttachmentsJob` may receive a new file or may reuse a `MediaFile` already stored on the node. After
`mediaManager.downloadPrivateMedia` returns and after `attachDownloadedRemoteMedia` links the returned
`MediaFileOwner` to the affected attachments:

1. Read `mediaFileOwner.getMediaFile().getRecognizedText()` from the actual persisted/reused local file rather than using
   only `PrivateMediaFileInfo.textContent`.
2. If that value is non-empty, invoke the local linked-object path in `OcrOperations` so headings and descriptions are
   recomputed and the appropriate posting/comment media-text and heading-update liberins are sent.
3. Keep sending `EntryMediaDownloadedLiberin` with the actual local title and recognized text so media-location and search
   subscribers receive the completed download update.
4. Do not send `MediaRecognizedTextUpdatedLiberin` from this path: no OCR completed here, and the downloaded remote
   attachment's lease notification originates from the node that performed OCR.

## Heading and description extraction

Refactor `HeadingExtractor.extractGalleryTexts` to handle both halves of `LocalRemoteMedia`:

- if a local `MediaFile` exists, retain all current local behavior and ignore remote recognized text;
- otherwise use `RemoteMediaFile.mimeType`, `title`, `hash`, and `recognizedText` with the same image/video/attachment and
  link-preview rules as local media;
- for a remote image, append non-empty recognized text and fall back to the picture marker when it is empty;
- preserve the existing length and ellipsis behavior for headings and descriptions.

## Search indexing and incremental updates

In `../moera-search`:

1. Include `RemoteMediaInfo.textContent` in `MediaTextUtil.buildMediaText(RemoteMediaInfo)` so full posting/comment ingest
   indexes remote OCR text together with the title.
2. Thread optional `mediaNodeName` through `SearchProcessor`, the posting/comment media-text update queue records,
   and `AttachmentIngest`.
3. Update `AttachmentRepository.updateMediaText` matching rules:
   - when the remote node name is absent, retain the existing local-media match;
   - when it is present, match the attachment by both `mediaNodeName` and `mediaId`.
4. Recompute the indexed posting/comment media text after either form of update, as today.
5. Update the node-side posting notification processor and `PostingOperations.updatePickedMediaText` to pass and use the
   optional origin node name when locating media, preventing collisions between equal media IDs on different nodes.

## Verification

Add focused tests covering:

- `RemoteMediaInfoUtil` exposes stored recognized text, while `RemoteMedia` conversion neither accepts nor overwrites it;
- local heading behavior remains unchanged and remote text is used only without a local `MediaFile`;
- lease notification construction/routing and synchronous rejection of an unknown exact lease;
- the receiving job updates only rows with the notified lease, is idempotent, clears caches, recomputes derived text, and
  emits remote-aware media-text/heading liberins once per affected current entry;
- metadata refresh from `PrivateMediaFileInfo` stores and propagates changed text;
- attachment download updates derived headings/descriptions and emits the required liberins when the returned local
  `MediaFile` already has recognized text, including when media storage reused an existing file;
- full search ingest includes remote text;
- incremental search updates distinguish local media from `(mediaNodeName, mediaId)` and update posting and comment
  indexes.

Then run:

1. `mvn -q -DskipTests compile` and the relevant `moera-node` tests;
2. the relevant `../moera-search` tests/build after installing the regenerated Java library;
3. `git diff --check` and `git status --short` in every touched repository.
