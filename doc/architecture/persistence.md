# Persistence and social model

## Database access

PostgreSQL is the authoritative store for node state. Spring Data JPA maps entities under `data`; repository interfaces
declare their queries explicitly. Hibernate schema generation is disabled. Flyway migrations under
`src/main/resources/db/migration` are the executable schema history.

Many operations use ordinary JPQL, pessimistic row locks, or update queries. Native SQL is reserved for database-level
coordination and bulk state transitions, including PostgreSQL advisory locks, `FOR UPDATE SKIP LOCKED`, partial
indexes, and trigger-maintained reference counts.

## Hosted-node partitioning

The `domains` table maps host names to node UUIDs. Tenant-owned entities carry `node_id`, and the active request or
background context supplies this ID to repository queries. Options are also stored per node. A single process can
therefore host multiple independent Moera identities while sharing application pools and content-addressed media.

## Entries and revisions

`Entry` is the common JPA base for `Posting` and `Comment`, using a discriminator to distinguish them. A comment's
`parent` is its posting. An entry stores stable identity, ownership and receiver snapshots, lifecycle timestamps,
permissions, counters, and a link to its current revision.

`EntryRevision` stores editable text and derived representations: source format, encoded body, sanitized body,
preview, heading, description, signatures, digest, attachments, and reaction totals. Revisions form a history through
their parent relation. Controllers query the current revision for ordinary reads and expose revision history through
dedicated endpoints.

`EntryAttachment` links a revision or draft to either a local `MediaFileOwner`, a `RemoteMediaFile`, or both after a
remote attachment has been downloaded locally. Its ordinal preserves attachment order, and `embedded` distinguishes
inline use from a separate attachment.

## Feeds and stories

`Story` is a materialized feed item. It belongs to a node and optional feed, has a `StoryType`, ordering moment,
publication state, read/viewed state, and a serialized summary. It may link to a local entry or retain remote node,
posting, comment, avatar, and parent-media snapshots.

Liberin receptors create and update stories after primary state changes. Creation in `Feed.INSTANT` is initiated by
liberin receptors and delegated to a focused component derived from `InstantsCreator`. This keeps instant creation on
the same post-commit path as events and notifications.

## Media child postings

An attached media object may have a child posting used for media-specific reactions and presentation. The child keeps
the actual parent media relation and a direct `Entry.parentMediaEntry` relation to the entry that owns the attachment.
Local media uses `parent_media_id`; remotely represented media can use `parent_remote_media_id`.

The direct parent-entry relation avoids inferring ownership through revisions, attachments, or receiver names. A unique
database index allows one local-media child per `(parent_media_entry_id, parent_media_id)`. Picks retain parent-entry
and remote-media context so asynchronously created child postings preserve the same relationship.

Attachment responses derive `MediaAttachment.postingId` in the context of the parent entry. The `.../attached`
endpoints return child postings only for media in the parent's current revision. The structural parent relation can
remain while an older or deleted revision still references the media.

Child permissions and metadata derive from the exact parent entry. If the parent is absent or no revision of that
parent references the media, application cleanup deletes the child through normal posting operations. Legacy children
that could not be linked during migration keep a null parent and restrictive permissions. Physical deletion of a
parent also has a database cascade as a final safety net.

The parent-entry ID is local structural metadata and is not part of posting fingerprints. Content signatures continue
to cover the parent media digest and attachment digests.

## Contacts, favors, and visited nodes

`Contact` is the local cache and relationship summary for another node. It stores profile snapshots, relationship
counts, a computed distance, and `visitCount`. `Favor` records decaying signals used by `FavorOperations` when
calculating contact distance.

The admin-only `/moera/api/people/visited` API records and searches node visits. Every POST adds a separate `VISITED`
favor with value `10` and a ten-day decay period, and increments the contact's visit count. DELETE removes all visited
favors for that node and resets the count. GET reuses `ContactSearch`, returns only positive visit counts, and sorts by
contact distance without fetching missing profile data during the request.

Every day, maintenance removes expired favors and recomputes positive visit counts from the remaining `VISITED`
records. Visited status reduces the contact distance by `0.25`, making the node closer in contact ordering.

## Historical identity metadata

The node's current source URI is the per-node option `profile.source-uri`. The API exposes it as `sourceUri` in profile
structures. Wherever a stored object captures a node full name, it also captures the corresponding optional source URI
from the same profile snapshot.

This rule applies to entries and their owner, receiver, and replied-to data; sources; drafts; reactions; contacts;
stories; locally remembered remote postings, comments, and reactions; and sheriff orders and complaints. These values
are historical metadata. A later profile refresh updates the contact snapshot but does not rewrite old postings,
reactions, or stories.

Source URI columns are nullable and limited to 1024 characters. Rows created before the fields were introduced remain
null unless a normal workflow later refreshes that specific record.

## Reference counts and deferred deletion

Database triggers maintain `usage_count` on shared objects such as media files and owners. When the count drops to
zero, a trigger assigns a deadline; a new reference clears it. This lets transactions update graph relationships while
scheduled cleanup performs expensive physical deletion later.

The same general pattern appears in durable work queues: primary transactions publish enough database state to make a
later action restartable, and workers delete queue records only after the external or derived work is complete.
