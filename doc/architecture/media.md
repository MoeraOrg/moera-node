# Media

[Architecture index](../architecture.md)

## Identity and ownership

| Concept | Responsibility |
| --- | --- |
| `MediaFile` | Shared content identity, digests, metadata, processing state, and physical locations |
| `MediaFileOwner` | One hosted node's private item, permissions, title, and malware/compression state |
| `RemoteMediaFile` | Origin node/media identity and received metadata, independently of a local copy |
| `MediaLease` | A peer's retained use/access relationship to locally owned media |

The API hides the first two entities: internal `MediaFileOwner.id` is the public **media file ID**, and internal
`MediaFile.id` is the public **media file hash**. Equal bytes can share a content row across hosted nodes without
sharing owner permissions. Previews are content files too. [Media storage](media-storage.md) owns physical copies,
ingest, serving, and deletion; this chapter owns use by content and processing workflows.

## Attachments and child postings

An attachment belongs to a revision or draft and can retain both remote origin metadata and a local owner after
download. A media child posting provides reactions/presentation in the context of the exact `parentMediaEntry` and
local or remote media identity. The same owner may have different children for different parents; local child
uniqueness is enforced per parent-entry/owner pair. Parent IDs are local structure, excluded from content fingerprints.

Current-attached listings match current revision attachments. Historical revisions can keep a child structurally alive;
when no revision of that parent uses the media, cleanup deletes the child through posting operations. Physical parent
deletion also cascades. Child permissions derive from that parent, not every entry using the same bytes.

Read conversion batches attachments and parent-scoped child identities. Cached, uncached, and write responses must
resolve the same child. Each response gets independent DTOs, grants, and refreshed paths; serialized caches must not
become permission authority.

Sources: [EntryOperations](../../src/main/java/org/moera/node/operations/EntryOperations.java),
[V270](../../src/main/resources/db/migration/V270__entries_parent_media_entry.sql), and
[EntryAttachmentsIT](../../src/test/java/org/moera/node/operations/EntryAttachmentsIT.java).

## Remote downloads

[MediaManager](../../src/main/java/org/moera/node/media/MediaManager.java) separates preparation from ownership:

1. `preparePrivateMedia()` checks committed ownership/cache state, serializes missing work with a process-local hash
   lock, and rechecks before downloading/verifying bytes. Content/cache state can commit independently; only scalar
   prepared data crosses the boundary.
2. `ownPreparedPrivateMedia()` reloads or creates ownership inside the final caller transaction, without a network call.
   Disappeared content requests retry. The caller revalidates its parent snapshot before publishing attachments.

A failed final write can leave reusable verified bytes but no partially attached graph. Remote leases are external
effects and cannot be undone by local rollback. Older download/avatar callers retain their own transaction boundaries.

Picker owner reuse includes parent entry, remote node/media identity, and prepared content across retained revisions.
Reintroduced identity reuses its owner; different remote IDs get different owners even if their bytes deduplicate.
This distinction is covered by
[MediaDownloadTransactionsIT](../../src/test/java/org/moera/node/media/MediaDownloadTransactionsIT.java).

## Processing

`VideoCompressionJob` reuses a completed result/existing job or schedules compression atomically with the request.
It validates job ownership and result state before work/publication, creates replacement owners under each affected
node's context, and retains original owner/draft references. Pending requested conversion prevents entry attachment
and S3 claiming of the source. Process deadlines bound stalled conversion; codec profiles and retry constants belong
in [video](../../src/main/java/org/moera/node/media/video).

`OcrJob` submits bytes or a direct URL to OCR.Space outside its write transaction. Results belong to shared content;
`OcrOperations` invalidates caches and recomputes affected headings/descriptions, emitting derived-text changes without
altering signed bodies. Client `RemoteMedia` input cannot set OCR text. Local content takes precedence over remote text,
including deduplication to an already recognized file.

Non-empty OCR results notify lease holders. `MediaProcessor` validates the local-node/sender/media/lease tuple and
schedules `MediaTextUpdatedJob`, which rechecks the relationship before changing remote metadata and derived text.
A removed lease makes delayed work a no-op; an empty local result does not clear text already propagated to peers.
Sources: [OcrOperations](../../src/main/java/org/moera/node/operations/OcrOperations.java) and
[MediaTextUpdatedJob](../../src/main/java/org/moera/node/rest/notification/MediaTextUpdatedJob.java).
