# Story publication synchronization

**Goal:** Notify posting subscribers when adding, editing, or deleting a story changes the earliest timeline
publication, and refresh a picked posting's publication date even when its content edit time is unchanged.

**Spec:** The user requested both fixes and suppression of posting notifications when the earliest date is unchanged.
The user also requires a story's feed to remain unchanged in PUT and the shared minimum-date method to be static in
StoryOperations. PUT accepts an omitted feed name or the existing name and rejects a different name.

**Architecture:** Compare the earliest timeline publication before and after a story mutation in its write transaction.
Use a dedicated posting-publication liberin to emit the existing posting event and subscriber notification after
commit. Picker updates the cached publication date independently of content revisions. Reuse the existing API and
notification payloads.

**Constraints:** Follow AGENTS.md. Keep getters and setters trivial. Keep documents under doc/ with lines at most
120 characters. Run Mockito tests with the JDK 25 Byte Buddy javaagent specified in AGENTS.md.

**Execution:** Implement in the current checkout. The requested fix is authorized; no merge or publish is needed.

## Tasks

- [x] Reproduce the unchanged-edit-time picker failure with a regression test and debugger observation.
- [x] Add regression cases for changes to the earliest timeline publication and changes that preserve it.
- [x] Extract the shared minimum-date calculation into StoryOperations.findEarliestPublishedAt,
  and compare dates in StoryController.put.
- [x] Add PostingPublishedAtUpdatedLiberin carrying the posting ID. PostingReceptor reloads the posting and sends
  PostingUpdatedEvent and PostingUpdatedNotification using its current visibility.
- [x] Refresh receiverPublishedAt in Picker's unchanged-content branch, emitting the dedicated liberin only on
  date changes, including when restoring a deleted posting.
- [x] Update doc/architecture/federation.md with the resulting publication-date synchronization behavior.
- [x] Reject feed changes in PUT before applying attributes, and remove feed-transfer handling.
- [x] Document the validation in the public API schema and regenerate dependent artifacts.
- [x] Rebuild java-moeralib, inspect all affected diffs, and run the project test suite with the required javaagent.
- [x] Add regression cases for earlier, equal, and later added publications; delete the earliest, tied, and last story.
- [x] Compare dates in StoryOperations.publish under the posting lock and notify once for a publication list.
- [x] Add StoryOperations.delete(Story) with the same lock and date comparison; use it for single and feed deletions.
- [x] Cover concurrent publication changes, update federation documentation, and run the full Maven test suite.
- [x] Move story attribute updates and update liberins to StoryOperations.update; share publication locking,
  refresh, and date-change notification helpers with publish and delete.
- [x] Verify the controller delegation and all existing regression cases after the refactoring.

## Review focus

- Moving the earliest story past the next publication, moving a later story earlier, and equal earliest dates.
- Changes outside timeline, unchanged timestamps, and edits to pinned/read/viewed state.
- Rejected feed changes leave all story attributes untouched; unchanged names remain accepted.
- Creation still accepts the requested feed and defaults to timeline when the feed is omitted.
- Null receiverPublishedAt, unchanged content revisions, and restoring a deleted cached posting.
- Visibility filtering, no repeat important-update notifications, and deletion before liberin delivery.

## Progress

- Source investigation confirmed that story edits send no posting notification, and Picker skips date updates
  when editedAt matches. A dedicated liberin avoids the content-edit receptor's important-update side effects.
- The picker regression failed in all four changed-date cases. A debugger logpoint observed editedAt=1000,
  incoming publication=900, and the stored publication still at epoch second 1000 after the unchanged-content branch.
  The test was paused briefly to retain the logpoint output; the session and agent breakpoints were cleaned up.
- Independent review identified concurrent edits of tied earliest stories as a missing case. Regression tests cover
  a publication snapshot read before acquiring the posting lock and a stale story timestamp.
- Relevant date edits acquire the shared posting lock and refresh the managed story before reading publications.
- Feed immutability regression tests failed before the fix: different names were accepted, and unchanged names
  unnecessarily acquired a posting lock and recalculated the moment. The handler now validates the name before any
  mutation and only considers publication timestamps for the earliest-date comparison.
- API regeneration changed only the schema, generated API documentation, and field comments in Java, Python, and
  TypeScript. Existing unrelated work in sibling repositories was preserved.
- Final verification: java-moeralib clean install passed all 77 tests; moera-node compile succeeded and its full
  suite passed all 528 tests with the required Byte Buddy javaagent. All affected repositories passed diff checks.
- Extended synchronization to added and deleted stories, including feed clearing. Regression tests reproduced the
  missing date liberins before the fixes. Publication snapshots are refreshed under the posting lock because bulk
  deletion can load several managed stories before acquiring it.
- Incremental review found no actionable issues. Persistence and concurrency tests use mocks; database lock/flush
  behavior was checked against the transaction and repository code, not against a running database.
- Final verification of the extended behavior: all 552 Maven tests passed with the required Byte Buddy javaagent.
- StoryController delegates mutation to StoryOperations.update. Publish, update, and delete share publication
  loading/refresh and date-change notification methods; update liberins use the existing transaction holding mechanism.
  Controller tests exercise the real operations component. Incremental review found no actionable issues, and all
  552 tests passed after the refactoring.
