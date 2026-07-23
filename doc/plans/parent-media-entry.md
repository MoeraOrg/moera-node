# Parent Media Entry Link Plan

## Goal

Add a direct link from a child media posting to the parent entry that attached the
media. Today a child posting has `entries.parent_media_id`, which points to
`media_file_owners`, and callers infer the parent entry through the chain:

```text
entries -> entry_revisions -> entry_attachments -> media_file_owners -> entries
```

For received remote postings this also requires filtering the child posting by
`entries.receiver_name` of the parent. This is fragile, and it will not work well
when future child entries are linked to non-media objects attached to the same
parent entry.

The new relation should let the node fetch child entries of a parent entry by
parent entry ID plus attached object ID, without using `receiver_name` as part of
the identity.

## Naming Decision

The requested column name is:

```text
entries.parent_media_entry_id
```

This is the final column name for this change.

## Database Changes

1. Add a nullable parent-entry column:

```sql
ALTER TABLE entries ADD COLUMN parent_media_entry_id uuid;

ALTER TABLE entries
    ADD CONSTRAINT entries_parent_media_entry_id_fkey
    FOREIGN KEY (parent_media_entry_id)
    REFERENCES entries(id)
    ON UPDATE CASCADE
    ON DELETE CASCADE;
```

2. Add lookup indexes for the new access pattern:

```sql
CREATE INDEX entries_parent_media_entry_id_idx
    ON entries(parent_media_entry_id);

CREATE INDEX entries_parent_media_entry_media_idx
    ON entries(parent_media_entry_id, parent_media_id);
```

If most lookups include `node_id`, use this index instead or in addition:

```sql
CREATE INDEX entries_node_id_parent_media_entry_media_idx
    ON entries(node_id, parent_media_entry_id, parent_media_id);
```

3. Replace the old uniqueness rules. Existing indexes:

```sql
entries_parent_media_id_null_idx
entries_parent_media_id_not_null_idx
entries_parent_media_id_receiver_name_idx
```

currently encode one child per media owner per receiver. The new uniqueness
should encode one child per parent entry and media owner:

```sql
CREATE UNIQUE INDEX entries_parent_media_entry_media_unique_idx
    ON entries(parent_media_entry_id, parent_media_id)
    WHERE parent_media_entry_id IS NOT NULL
      AND parent_media_id IS NOT NULL;
```

Do not keep a legacy uniqueness fallback for rows where
`parent_media_entry_id` is null. Null backfill results are accepted as historical
inconsistencies.

After this change, uniqueness on `(parent_media_id, receiver_name)` must not be
enforced anymore. The non-unique `(parent_media_id, receiver_name)` index may
also be removed unless a remaining measured query still needs it.

## Backfill Strategy

Backfill existing media child postings by matching each child row to a parent
entry that has the same media owner attached to any revision. This includes
deleted revisions. The parent link is entry-level historical context, not only a
link to the current revision.

Candidate relation:

```sql
child.parent_media_id = entry_attachments.media_file_owner_id
entry_revisions.id = entry_attachments.entry_revision_id
parent.id = entry_revisions.entry_id
parent.node_id = child.node_id
parent.id <> child.id
parent.receiver_name IS NOT DISTINCT FROM child.receiver_name
```

Write `parent_media_entry_id` when one or more candidate parents exist. If
several candidates exist, choose the first candidate ordered by
`parent.created_at, parent.id`. There is no business rule for resolving this
ambiguity, and choosing any one deterministic parent is acceptable.

Rows with zero candidates must be left unchanged. These are the important
preflight classes:

- `NO_PARENT`: a child posting references media, but no parent entry attaches
  that media in any revision.
- `DUPLICATE_BACKFILLED_PAIR`: several child postings would map to the same
  `(parent_media_entry_id, parent_media_id)` pair.

`AMBIGUOUS_PARENT` is not a backfill failure. It should be counted for
visibility if useful, but the migration should resolve it by selecting one
candidate.

No legacy receiver-name fallback is required after the migration. Legacy rows
that cannot be backfilled may remain with `parent_media_entry_id = NULL`.

## Java Persistence Model

1. Add a relation to `Entry`:

```java
@ManyToOne(fetch = FetchType.LAZY)
private Entry parentMediaEntry;
```

2. Add `getParentMediaEntry()` and `setParentMediaEntry(...)`.

3. Keep the existing `Entry.parent` relation unchanged. It is used for comments
and affects comment totals, comment permissions, reactions, and story logic.

4. Keep `Entry.parentMedia` unchanged for the actual media owner relation.

## Child Posting Creation

Current media-posting creation only receives a `MediaFileOwner`, then sets
`parentMedia`.

Change the creation path so callers pass both:

```text
MediaFileOwner parentMedia
Entry parentMediaEntry
```

Update:

- `PostingOperations.newPosting(MediaFileOwner mediaFileOwner)`
- media upload creation in `MediaController`
- picker/download code paths that create child postings from attached remote
  media
- posting/comment update code that updates metadata and permissions of media
  child postings

Do not create child postings for standalone uploaded media files. Create a child
posting only when the media is attached to an entry. At creation time, set both
`parent_media_id` and `parent_media_entry_id`.

## Attachment Query Changes

Replace the current attached-posting queries that join through
`media_file_owners.postings` and filter by `receiverName`.

New query shape:

```text
parent current revision, for current-attachment endpoints
  -> entry_attachments
  -> child entries
       child.parent_media_entry_id = parent.id
       child.parent_media_id = entry_attachments.media_file_owner_id
```

Update:

- `EntryAttachmentRepository.findOwnAttachedPostings(...)`
- `EntryAttachmentRepository.findReceivedAttachedPostings(...)`
- `PostingController.getAttached(...)`
- `CommentController.getAttached(...)`

After this change, `PostingController.getAttached(...)` should not need the
`posting.isOriginal()` branch for attached child lookup.

Important behavior: `GET .../attached` should only return child postings for
media attached to the parent entry's current revision. If media is removed from
the current revision, the child posting should disappear from this response even
if the child still has `parent_media_entry_id`.

This current-revision filter is specific to endpoints that list currently
attached media. It must not be used as the criterion for setting or preserving
`parent_media_entry_id`. A child posting remains linked to the parent entry as
long as the media owner is attached to any revision of that entry, including a
deleted revision.

## Permission and Metadata Propagation

Currently media child posting permissions are derived through
`MediaFileOwner`. This is not correct after the parent-entry link is added.
The parent entry must become the source of truth for child posting permissions.

`MediaOperations.updatePermissions(MediaFileOwner)` currently groups parent
entries by `receiverName` when updating child postings. This must be replaced
or refactored so child posting permissions are calculated from
`parentMediaEntry`, not from `MediaFileOwner`.

For each media child posting:

1. If `posting.parentMediaEntry` is null, keep the child posting but set
   restrictive permissions:
   - posting view: `secret`
   - posting edit: `owner`
   - posting delete: `secret`
   - viewing or deleting comments/reactions: `admin`
   - adding or editing comments/reactions: `none`
2. Otherwise, find the exact parent entry referenced by
   `posting.parentMediaEntry`.
3. Verify whether the parent entry still attaches `posting.parentMedia` in any
   revision.
4. Copy rejected-reaction metadata and derive posting, comment, and reaction
   permissions from that parent entry.
5. If the parent entry is gone or no revision of that parent entry attaches the
   media anymore, delete the child posting through `PostingOperations.deletePosting()`
   and send the corresponding deletion liberin.

Also review calls that update media-child metadata during posting/comment edits.
They currently find a child via `mfo.getPosting(receiverName)` or
`mfo.getPosting(null)`, which becomes ambiguous when the same media owner is
attached to multiple parent entries.

## Public API and moeralib

This is a protocol-visible change if clients need to understand the new
relationship.

Recommended API additions:

1. Add `PostingInfo.parentMediaEntryId`.
   - Meaning: ID of the entry that owns the attachment this posting is linked
     to.
   - Optional, nullable.

2. Add `MediaAttachment.postingId`.
   - Meaning: ID of the child posting linked to this attachment, if any.
   - Return it whenever the child posting exists, regardless of whether the
     requesting client can view the child posting.
   - This is attachment-contextual, so it correctly supports the same media
     owner being attached to different parent entries.

3. Remove `PrivateMediaFileInfo.postingId`.
   - It is media-owner scoped and becomes incorrect when the same media owner
     may have different child postings in different parent-entry contexts.

4. Update the API schema in `moeraorg.github.io/_data/node_api.yml`.

5. Regenerate and release:
   - `java-moeralib`
   - `typescript-moeralib`
   - `python-moeralib`

6. Update `moera-node` to depend on the regenerated `moeralib`.

Backward compatibility:

- Adding optional response fields is safe for tolerant clients.
- Generated clients with `additionalProperties: false` may reject unexpected
  fields unless regenerated, so coordinate releases.

## Remote and Async Workflows

Remote media child postings may be created later by picker jobs. Those jobs need
the parent entry context.

Update `picks` or the equivalent job state to store:

```text
parent_media_entry_id
media_file_owner_id
remote_posting_id
```

Without this, the async download can still create a child posting for the media,
but it cannot link the child to the parent entry deterministically.

Also review:

- remote posting update jobs
- failed remote media reaction jobs
- media reaction notifications and stories that carry parent posting/media IDs

The remote protocol can continue to use remote posting ID plus remote media ID
for federation, while the local DB uses `parent_media_entry_id` for stable local
joins.

## Fingerprints and Signatures

Do not include `parent_media_entry_id` in posting fingerprints unless protocol
semantics require it.

Current fingerprints include the parent media digest and attached media digests.
The parent entry ID is local structural metadata, not content identity. Adding it
to fingerprints would invalidate existing signatures and complicate federation.

## Purge and Lifecycle

Review cleanup jobs:

- `PostingOperations.purgeUnlinked()`
- media owner cleanup
- deleted posting/comment cleanup
- media permission refresh

Define lifecycle behavior for child postings when:

- the parent entry is deleted,
- the parent revision no longer attaches the media,
- the media owner is deleted,
- the child posting itself is deleted.

When a parent entry is soft-deleted, soft-delete all child postings linked to it
through `PostingOperations.deletePosting()` and send the corresponding deletion
liberins.

When media is removed from a parent entry's current revision:

- If the same media owner is still attached to any other revision of the same
  parent entry, preserve the child posting.
- If the same media owner is no longer attached to any revision of the same
  parent entry, delete the child posting through `PostingOperations.deletePosting()`
  and send the corresponding deletion liberin.

The FK `ON DELETE CASCADE` from `parent_media_entry_id` to `entries(id)` will
delete child entries when the parent entry is physically deleted. This is
acceptable as a database safety net, but the preferred application behavior is
to delete child postings through `PostingOperations.deletePosting()` and send
the corresponding deletion liberin before physical deletion reaches the FK.
Soft deletes also need application handling.

Existing standalone child postings that cannot be linked to a parent entry
should remain with `parent_media_entry_id = NULL` and the restrictive permissions
defined above.

## Tests

Add focused coverage for:

1. Local posting with attached media returns child postings through
   `GET /postings/{id}/attached`.
2. Received remote posting with attached media no longer depends on
   `receiverName` matching.
3. Comment attachment child postings are returned through
   `GET /postings/{postingId}/comments/{commentId}/attached`.
4. Same media owner attached to two different parent entries can have distinct
   child postings.
5. Removing media from the parent current revision removes it from current
   attached child lookup, but preserves the child posting and
   `parent_media_entry_id` when the media is still attached to another revision
   of the same parent entry.
6. Permission updates use the exact parent entry instead of all entries with the
   same media owner and receiver.
7. Removing media from all revisions of the parent entry deletes the child
   posting through `PostingOperations.deletePosting()` and sends the deletion
   liberin.
8. Backfill handles:
   - one candidate parent,
   - zero candidates,
   - multiple candidates by choosing one deterministic parent,
   - candidates found through deleted revisions,
   - duplicate resulting pairs.

## Rollout Plan

1. Add nullable column, FK, and non-unique lookup indexes.
2. Deploy code that writes `parent_media_entry_id` for new child postings.
3. Run production preflight checks for zero-candidate and duplicate backfill
   cases. Ambiguous candidates are acceptable and should be resolved
   deterministically.
4. Backfill rows that have at least one candidate parent.
5. Add the new unique index.
6. Remove old parent-media uniqueness indexes that include `receiver_name`.
7. Regenerate and release moeralib clients before exposing or relying on new API
   fields in external clients.

## Main Risks

- Existing data may not identify any parent entry for some child postings; those
  rows will remain with `parent_media_entry_id = NULL`.
- Existing data may identify several possible parent entries; choosing one may
  attach a legacy child posting to a plausible but arbitrary parent.
- If the migration does not drop the obsolete `(parent_media_id, receiver_name)`
  uniqueness/indexes, production may still reject valid future rows.
- Any remaining receiver-name-based permission lookup may apply permissions from
  unrelated parent entries instead of only the linked `parent_media_entry_id`.
- Async picker jobs may create unlinked child postings if they do not persist
  parent-entry context.
- Strict generated clients may reject new response fields before moeralib is
  regenerated.
- Cascading deletes through the new FK may delete more than expected if physical
  deletion paths are not reviewed.
