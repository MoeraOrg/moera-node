# Story visibility implementation plan

**Goal:** Give stories independent optional viewing permissions and preserve remote posting identity.

**Architecture:** Persist a principal on each story, defaulting to `unset`. An explicit principal controls story
visibility independently of its entry; posting conversion still checks the entry's permissions. Existing permission
behavior remains in effect for `unset`. Story ownership belongs to the local node, so `private` permits its admin.

**Specification:** The task in this session defines the API fields, migration selection, and visibility behavior.

**Constraints:** Preserve existing sibling-repository changes. Generate API artifacts from `node_api.yml`. Keep
permission validation identical to posting viewing permissions, with `unset` added. Use the Byte Buddy agent for tests.

- [x] Extend the API schema with `StoryAttributes.operations` and `StoryOperations.view`; regenerate and build Java.
- [x] Test and implement principal validation/conversion, remote identity publication, and story/event serialization.
- [x] Add the column and backfill migrations; verify timeline/non-timeline, original/copied, and non-posting cases.
- [x] Apply story permissions to feed queries, direct story reads, events, and public timeline/RSS filtering.
- [x] Test independent story/posting permissions, private/subscribed/friend-group access, and pagination in PostgreSQL.
- [x] Update persistence architecture documentation, run focused and broader checks, and review all changed diffs.

## Review focus

- A visible story must not leak an unreadable posting, its attachments, or its linked postings.
- Stories without entries must respect explicit visibility.
- SQL filtering must preserve explicit-public stories pointing to private postings, including pagination.
- Default `unset` behavior and admin feed restrictions must remain intact.
- Backfills must preserve unrelated story types and copy remote IDs outside timeline as well.

## Verification

- Regenerated API artifacts and successfully ran `mvn clean install` in `java-moeralib`.
- Full `mvn test` passed with the required Byte Buddy agent.
- PostgreSQL verification passed: 18 story visibility/migration tests and 14 linked-posting regression tests.
- React and TypeScript library type checks passed; generated validators accept stories without embedded postings.
- Source/generated API YAML and generated Python syntax validated; all touched repositories pass `git diff --check`.
- HTML documentation build is unavailable: the Bundler executable targets missing Ruby 3.0, and the installed Ruby
  has no Jekyll executable. No dependency installation was needed for code verification.
- Independent review identified reference visibility, posting-change notifications, and story visibility transition
  gaps. Regression tests reproduced each issue before the fixes and passed afterward.
