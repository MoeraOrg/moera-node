# Batch media attachments implementation plan

> Execution: `superpowers:executing-plans`, continuing the existing workspace and uncommitted attachment optimization.

**Goal:** Batch attachment reads for lists and cover the remaining GET handlers and drafts.

**Architecture:** Read handlers prepare a request-local provider for their selected revisions. Valid attachment caches
remain usable; missing caches share attachment and posting-ID queries. Drafts use a separate provider preserving their
null-parent posting lookup and visibility rules. Write responses continue using the existing relation-based provider.

**Tech stack:** Java, Spring Data JPA, Hibernate, PostgreSQL, JUnit.

**Spec:** The design in the conversation, approved by “Сделай это”.

## Constraints

- Continue in the user's current workspace; preserve the previous uncommitted changes. Do not commit automatically.
- Explicit repository queries, no public API changes, no changes to generated schema documentation.
- Keep attachment order, previews, local-media priority, remote visibility, grants and cache-source validation.
- Use the configured Byte Buddy agent for Maven tests on JDK 25.
- Keep transaction boundaries and all write-response paths intact.

## Review focus

- Repeated revisions share database reads but receive independent response objects and grants.
- Empty and cached-only batches do not issue attachment queries.
- Invalid or mismatched caches fall back to the batch database loader.
- Drafts retain invalid remote attachments and select only null-parent media postings.
- Pagination selects drafts before fetching attachment collections.

## Task 1: Revision batches

- [x] Extend `EntryAttachmentsIT`: constant query count for 1 and 20 revisions, repeated IDs, empty revisions,
  cached-only and mixed batches, invalid cache fallback, response isolation.
- [x] Observe failing integration tests before implementing batching.
- [x] Add collection queries to `EntryAttachmentRepository` and a batch `MediaAttachmentsProvider.database` factory.
- [x] Add `EntryOperations.prepareMediaAttachments(Collection<EntryRevision>)`, retaining cache handling and generating
  response DTOs on demand with each caller's grant supplier.
- [x] Run `EntryAttachmentsIT` against the isolated PostgreSQL database; expect zero failures.

## Task 2: Draft batches

- [x] Extend integration tests for null-parent local/remote posting IDs, local precedence, visibility and pagination.
- [x] Add `DraftAttachmentsProvider`, explicit draft attachment/ID queries and a `DraftInfoUtil.build` overload.
- [x] Change draft GET repositories to select drafts without collection fetches; batch their attachments separately.
- [x] Keep the existing draft builder overload for write responses.
- [x] Run integration tests; expect zero failures.

## Task 3: Read-handler integration and verification

- [x] Prepare batch providers in feed slices, comment slices, story lists, external-URL posting lookup,
  posting/comment revision lists, deleted-posting lists and built-in Web UI lists.
- [x] Use `EntryOperations` for single comment-revision reads and the draft provider for single draft reads.
- [x] Update `doc/architecture/media.md` to describe the implemented read paths.
- [x] Run integration tests and the default Maven suite with the Byte Buddy agent;
  run Checkstyle and `git diff --check`.
- [x] Review the complete diff, resolve substantive findings, and report verification and remaining limits.

## Progress

- Initial state: previous single-revision optimization present;
  429 default tests and 6 PostgreSQL tests passed previously.
- Design approval already supplied by the user; implementation proceeds without a further approval gate.

- Revision batch tests: observed 3 expected query-count failures, then all 10 tests passed.
- Draft tests: observed collection-loading and database-pagination failures, then 17 integration tests passed.
- Added grant independence and draft GET integration coverage; found JSON `null` cache deserialization failure.
  Added a null guard; the regression test now passes.
- Final independent review: no actionable findings. Existing eager associations can still cause additional queries;
  the two-query contract concerns attachment and posting-ID loading. Production latency is not measured here.
- Review ruling: existing cache freshness rules stay unchanged. JSON `null` handling is improved and regression-tested.

- Final verification: 20 PostgreSQL integration tests and 429 default Maven tests passed, zero failures or errors.
  Checkstyle and `git diff --check` passed. No commits were created.
- Integration command: `mvn -o -Dskip.installnodenpm -Dskip.npm -Dtest=EntryAttachmentsIT,DraftControllerIT`
  `-Dmoera.test.database=jdbc:postgresql://127.0.0.1:55439/postgres -DargLine=<Byte Buddy agent> test`.
- Default-suite command: `mvn -o -Dskip.installnodenpm -Dskip.npm -DargLine=<Byte Buddy agent> test`.
  Both commands used the agent path required by `AGENTS.md`.
