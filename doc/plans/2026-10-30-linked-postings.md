# Linked postings implementation plan

**Goal:** Persist posting references per revision, fetch remote targets, retain linked copies, and expose visible
targets.

**Architecture:** `LinkedEntry` belongs to an `EntryRevision` and optionally resolves to an `Entry`.
`LinkedEntryOperations` extracts references after the revision body is finalized. Local targets resolve immediately;
`LinkedEntryOperations` submits remote picks through `PickerPool.pickAfterCommit()`. Picks retain linked-entry identity
through redirects and resolve it in the transaction that stores the downloaded posting.
API conversion includes all levels of visible targets, omitting repeated ancestors in each path.

**Constraints:** Follow `AGENTS.md`; keep documents under `doc/`; do not edit `doc/create_tables.sql`.
Use explicit repository queries, field injection, and the JDK 25 Byte Buddy agent for Mockito tests.
The user request is the specification. Work in the supplied checkout; preserve existing changes in sibling repositories.
No backfill of old revisions is requested. Nested linked postings are expanded with path-specific cycle detection.

## Tasks

- [x] Add migration V325, `LinkedEntry`, revision relationship, and explicit repository queries.
  Test cascades, nulling deleted targets, tenant isolation, and exclusion from unused-posting cleanup.
- [x] Add `LinkedEntryOperations.create(EntryRevision)` and submit picks through `PickerPool.pickAfterCommit()`.
  Call from local revision creation and picker revision creation after the body is finalized.
  Test duplicate references, local/remote targets, media exclusions, commit/rollback behavior, and redirects.
- [x] Read saved references in `PostingReceptor`; preserve visibility and favor behavior.
  Adapt existing receptor tests and prove notifications no longer require parsing the body.
- [x] Add optional `PostingInfo.linkedPostings` to the source API schema, run `../update-api`, and rebuild
  java-moeralib.
  Include visible, live targets with their attachments and recursive nesting; test permissions and cycles.
- [x] Update persistence/federation architecture, run Maven tests and isolated PostgreSQL integration checks,
  review the complete diff, and run `git diff --check` in each affected repository.

## Progress

- Inspected local and picker revision creation, reference notifications, cleanup, API conversion, and permissions.
- Existing unrelated edits are present in API/library/relay sibling repositories and must be preserved.

- Implemented all tasks. `PostingReceptor` reads references by tenant and revision ID in its own transaction;
  notification metadata conversion omits linked targets to avoid traversing detached entities.
- At the user's request, pick submissions use the ordinary `PickerPool.pick()` path, with no special job or handling
  of pick persistence failures. Redirects back to the local node resolve to the existing original. Media exclusions also
  cover legacy children without a parent entry.
- API generators now support self-referential structures; client body decoders handle linked postings recursively.
- Initial validation: 611 node tests passed, including 42 PostgreSQL integration tests. All migrations through V325
  applied to an empty isolated database. Java library: 77 tests; TypeScript library: 13; Python library: 11;
  React regression: 3.
  TypeScript builds/typechecks, API regeneration, and diff whitespace checks passed.
- Independent review findings were reproduced with failing regression tests and fixed; final review found no remaining
  critical or important issues. The temporary PostgreSQL server was stopped.
- After moving the transaction callback into `PickerPool.pickAfterCommit(Pick)`, all 62 affected tests passed, including
  7 PostgreSQL integration tests. Checks cover immediate submission outside a transaction, commit, and rollback, and
  verify that picks can be saved in a separate transaction after the revision commits.

## Batched response loading

The response layer will load linked targets explicitly, using the source revision IDs. `PostingInfoUtil` will only
convert the supplied posting. `PostingInfoOperations` will prepare linked responses and client metadata for both a
single posting and a feed slice; recursive expansion uses the shared batches described below.

- [x] Add explicit bulk queries for references, full target postings, and their top-level stories. Reuse the existing
  bulk attachment provider. Verify that loading distinct targets does not add one query per target or source posting.
- [x] Fill reactions and blockings in batches, preserving the permissions of the single-posting API. Group sheriff
  user-list lookups by sheriff and apply marks only to the relevant targets. Test anonymous and restricted callers,
  local and remote targets, repeated remote IDs on different nodes, global and individual blockings, and cycles.
- [x] Connect posting, story, deleted-posting, and feed responses to the explicit loader. Keep media children and
  notification metadata free of linked-posting expansion. Update architecture documentation and run affected tests.

Validation: 622 tests passed (574 unit tests and 48 PostgreSQL integration tests). In fresh transactions, feed slices
with 1 and 20 distinct linked targets both require 7 SQL statements for ordinary or media targets, 8 for compressed
attachments, and 9 for remote targets with the owner's reactions. The regressions reproduced per-target fetches before
the missing joins were added. Independent review found no remaining actionable issues.

## Recursive response loading

The user requires every level of linked postings, cutting only a target already present in the current ancestor path.
Load references and previously unseen visible postings breadth-first across the whole response. Prepare stories,
attachments, reactions, blockings, and sheriff marks once for the distinct targets. Build each response tree from
shallow copies of these prepared DTOs, with separate linked-posting lists and an ancestor set for each traversal.
Use an explicit traversal stack so deep acyclic chains do not exhaust the Java call stack.

- [x] Update `PostingInfoOperationsTest` and `PostingInfoLinkedPostingsTest` for chains, cycles, shared targets,
  independent roots and branches, denied permissions, deep chains, and metadata at every level. Verify failures.
- [x] Replace the one-level loader in `PostingInfoOperations` with batched graph discovery and path-specific expansion.
- [x] Extend `LinkedEntryIT` to compare SQL counts for recursive feed slices with 1 and 20 source postings.
- [x] Update the API field description and persistence architecture. Regenerate API artifacts and rebuild java-moeralib.
- [x] Run unit tests and PostgreSQL linked-entry regressions; review the recursive loader and check diff whitespace.

Review fixes preserve omission of linked postings for legacy media children, including when a media posting is both
a response root and a linked target. The cycle regressions serialize single-posting and slice responses through
Spring's HTTP JSON converter. Construction is also tested with 2,000 linked postings. Jackson's general serialization
limit of 500 JSON levels is unchanged; changing the transport and client parser limits is outside this loader change.

Final validation: 598 node tests passed (584 unit tests and 14 PostgreSQL linked-entry tests), plus 77 java-moeralib
tests. Fresh-transaction slices with 1 and 20 source postings both require 12 queries for three-level chains or cycles,
8 for one-level ordinary targets, 7 for media targets, 9 for compressed attachments, and 10 with the owner's remote
reactions. Final review found no remaining actionable loader findings.
