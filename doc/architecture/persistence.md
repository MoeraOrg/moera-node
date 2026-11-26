# Persistence and content model

[Architecture index](../architecture.md)

## Ownership and lifecycle

PostgreSQL holds local authority, remote copies, derived records, and pending work. Entities/explicit queries live in
[data](../../src/main/java/org/moera/node/data); Flyway owns schema evolution and trigger-maintained relationships.
Tenant state belongs to a local node UUID; shared media bytes are the exception described in [media](media.md).

| Concept | Owns |
| --- | --- |
| `Entry` (`Posting` or `Comment`) | Identity, author/receiver snapshots, permissions, counters, current revision |
| `EntryRevision` | Versioned body/signature, derived text, attachments, outgoing references, revision reactions |
| `Draft` | Unpublished editable content and draft attachments, independently of entries |
| `EntryAttachment` | Ordered/embedded use of local ownership and/or remote media metadata by a revision or draft |

A comment's parent is its posting. An original has no `receiverName`; a picked copy retains the authoritative remote
node and entry/revision IDs. Authorship is separate: a remotely authored entry can be an original received here.

[PostingOperations](../../src/main/java/org/moera/node/operations/PostingOperations.java) and `CommentOperations` own
revision creation, restoration, and soft deletion; retention cleanup performs physical deletion later. An unsigned
latest revision can be replaced rather than retained as signed history. Sanitized HTML and derived OCR text must not
replace the body covered by a signature. Draft update/deletion use the same tenant-scoped row lock, preventing an
update from attaching media after deletion wins.

### Revision timestamp discrepancy

`AGENTS.md` equates non-null revision `deleted_at` with the current revision. Mutation code instead stamps superseded
revisions, creates the new current revision without that timestamp, and stamps it on entry deletion; picker restoration
clears it again. This discrepancy remains unresolved. Select current content through `Entry.currentRevision`, not a
new timestamp shortcut. The restoration behavior is asserted in
[PickerTest](../../src/test/java/org/moera/node/picker/PickerTest.java).

## References and sharings

`LinkedEntry` belongs to a source revision and stores destination identity plus optional local resolution. Extraction
runs after the body is finalized; unresolved remote targets may be picked without feed publication. Deleting
the source revision cascades to its links/picks; physical target deletion clears resolution but preserves destination
identity. Resolved links from any retained revision prevent unused-posting cleanup. When an original is deleted,
`posting.referenced.hide-on-delete` (enabled by default) preserves a referenced picked copy with admin-only visibility;
`posting.picked.hide-on-delete` applies that retention to all picked copies. Otherwise the copy is soft-deleted.

`PostingInfoOperations` expands only visible live targets, with each target's own permissions. Cycles are cut per
ancestor branch, preserving shared descendants in independent branches. Media children do not own outgoing references.
See [LinkedEntryOperations](../../src/main/java/org/moera/node/operations/LinkedEntryOperations.java),
[V325](../../src/main/resources/db/migration/V325__linked_entries.sql), and
[PostingInfoOperationsTest](../../src/test/java/org/moera/node/operations/PostingInfoOperationsTest.java).

`EntrySharing` records federated sharings of an original posting and may retain a specific revision. Identity includes
the sender, type, optional revision, and remote story/posting references. Picked copies retain the received total.

A reference whose destination name matches its source author is counted locally from `LinkedEntry`. Only the active
signed content of a live source posting contributes, with one sharing per source and target. The list merges these
references with federated sharings using unique moments, and checks source visibility before disclosing a local
reference. The node assigns the local moments when saving or resolving links. Sharing changes update the original
posting's stored total under its lock. Local link changes refresh the total when the node handles the source posting
change. Read operations use the stored value under the target's counter permission.
See [SharingOperations](../../src/main/java/org/moera/node/operations/SharingOperations.java),
[V333](../../src/main/resources/db/migration/V333__own_posting_sharings.sql), and
[Federation](federation.md#sharing-flow) for federated delivery.

## Relationships and snapshots

A feed `UserSubscription` connects a remote feed to one local destination. The database enforces uniqueness per hosted
node, remote feed and destination. Creating or removing a feed subscription updates the contact's subscription count
in the same transaction. Multiple destinations share one active `Subscription` for the remote feed. PostgreSQL counts
its local references so that removing one destination preserves delivery to the others. A `Subscriber` records a
peer's interest here.
[Subscription schema](../../src/main/resources/db/migration/V331__feed_subscription_identity.sql)

Friend groups and grants add authorization relationships. `OwnPosting`, `OwnComment`, and `OwnReaction` retain knowledge
of actions stored remotely. Contacts cache profile and relationship summaries. Favors represent decaying interaction
signals used for contact distance. Blocking, sheriff orders, complaints, and malware lists remain separate policy state.

Historical profile snapshots belong to the stored object: full name and source URI are captured together, and a later
contact refresh does not rewrite old content. Source URI is opaque identity metadata, distinct from external source
references on content/publications. [Stories and feeds](stories-and-feeds.md) defines publication ownership and cleanup;
[media storage](media-storage.md#cleanup) defines reference-counted physical deletion.
