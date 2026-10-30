# Posting references implementation plan

Goal: notify the author when an original posting links to their posting through a universal Moera URL.
Extract links from the final HTML using Jsoup, send `REFER` sharing notifications, create `posting-referred` instants,
and record reference favors.
Search integration is outside this change.

The existing sharing lifecycle will own incoming references. The existing posting liberin receptor will compare outgoing
references on creation, editing, deletion, and restoration. This keeps reference discovery independent of mentions
and reuses the sharing transaction, avatar job, event, and grouped-instant mechanisms.

- [x] Add `SharingType.REFER` (ordinal 1) and `StoryType.POSTING_REFERRED` (ordinal 51) to the API source;
  regenerate all libraries and rebuild Java moeralib. Preserve every existing ordinal.
- [x] Add `PostingReferencesExtractor.extract(Body)` returning a deduplicated set of `(nodeName, postingId)` records.
  Parse the final HTML with Jsoup like `MentionsExtractor`, taking URLs from anchor `href` attributes.
  Reuse `UniversalLocation`, normalize names, restrict hosts to `moera.page` and `web.moera.org`, and reject comment,
  media, malformed, and other-page targets.
  Test HTML entities, duplicate spellings, encoded components, queries, and malformed links; ignore URLs outside
  anchor targets and the posting's HTML text body.
- [x] Extend `PostingReceptor` for original-posting lifecycle liberins. Compare references and recipient access
  using the same signature and visibility rules as mentions. Send notifications with the source posting ID and no
  revision/story reference. Record `REFERENCE`/`UNREFERENCE` favors on actual link changes, independently of visibility.
  Append favor enum members with values +10/-10 and a seven-day decay. Test lifecycle, access, self-links, and favors.
- [x] Select the instant type from the sharing type in `PostingSharingInstants`; preserve grouping, deduplication,
  blocking, moderation metadata, and deletion. Serialize the latest referring posting as the navigation target and
  retain the same priority. Test separate groups, duplicate delivery, cleanup, and API/event payloads.
- [x] Add the React summary, repeat icon, target, and translations in English, Polish, Russian, and Ukrainian.
  Re-import relay presentation, synchronize its locale keys, and reuse the existing Android `ms_repeat` drawable.
- [x] Select sharing-dialog links by type: references open the source posting, republishing opens the remote feed
  at the story moment. Derive instant text in the display state without mutating the incoming API/event story.
  Cover dialog links, instant rendering, unchanged wire data, and regeneration after a language change.
- [x] Update federation and persistence architecture chapters to describe the implemented behavior.
- [x] Run focused then full Maven tests with the configured Byte Buddy agent, React tests/build, TypeScript library
  and relay checks, Android build, API documentation build where available, and diff checks in all touched repositories.

Acceptance example: a post by `alice_0` contains two HTML links with spellings of a universal URL to `bob_0/post/abc`.
Bob receives one `REFER` sharing with Alice's source posting ID and one `posting-referred` substory. Editing only the
URL spelling adds nothing. Removing the link sends one unsharing, removes its substory, and records the inverse favor.
Adding `?comment=def` does not create a posting reference.

Review focus: privacy-only changes must not duplicate favors; query parameters must not let comment links through;
reference groups must remain separate from republishing groups; deletion must clean the proper type after the sharing
row is removed; imported relay changes must preserve its pre-existing working-tree changes.

Baseline: node and generated libraries clean; React has an unrelated untracked archive; relay has pre-existing edits
to its importer, imported presentation, locales, and index template. Work continues in the shared checkout because
the generators depend on fixed sibling paths. No commits or publication are part of this task.

Implementation finding: `LiberinManager` accepts one receptor per liberin type. Reference discovery therefore
runs from the existing `PostingReceptor` alongside mentions. The Java library keeps its existing snapshot version;
the node now depends on that locally built snapshot, which contains the new enum members.

Verification progress:

- Existing focused sharing tests passed before changes.
- New extraction/receptor tests first failed for missing implementation; receiver tests then demonstrated the wrong
  instant type and missing navigation payload before those paths were implemented.
- PostgreSQL integration: 19 tests passed, including both sharing types, group expiry, blocking, duplicate delivery,
  and reference-only cleanup. Full API migrations ran in a new isolated PostgreSQL instance.
- Extraction tests cover complete query strings, comment parameters, adjacent links, and encoded query values.
  The switch to Jsoup first failed 15 cases against the regex implementation, including URLs outside anchors,
  HTML entities, and removing a reference by replacing its anchor with plain text. All 62 focused extraction/receptor
  tests passed after the switch, followed by the full Maven suite.
- The server serialization regression passed for both sharing types: API responses, added/updated events, and event
  copies preserve `summaryData` and omit legacy `summary`. All 20 `StoryInfoUtilTest` cases passed.
- A diagnostic using the actual earlier client formatter reproduced the empty summary for an unknown instant type.
  The reducer cached that computed value in the incoming story and reused it even after the formatter supported the
  type. Both that diagnostic and the permanent language-change regression failed before removing the mutation and
  passed afterwards. The permanent tests consume JSON-shaped stories without a preassigned empty summary.
- React: all 365 tests passed, including the sharing-dialog links and full instant-rendering regressions.
  The production build passed. TypeScript library: all 10 tests passed. Relay type check
  passed against the local linked library. Android debug assembly passed; the final icon reuses `ms_repeat`.
- API site build could not run: the default Bundler executable requires missing Ruby 3.0; Bundler 3.2 cannot find
  Jekyll. Generated schemas and documentation data were inspected and all existing ordinals were verified unchanged.
- Locale changes remain local; no external translation publication is included.

Node verification: the earlier full Maven suite passed (498 tests, zero failures/errors/skips), in addition to the
19 PostgreSQL sharing integration tests. The added serialization cases passed in the 20-test focused suite.
All eight repositories passed `git diff --check`; changed documentation meets the
120-character limit. React production compilation reports existing dependency/CSS warnings; the build succeeds.
Deployment requires the regenerated Java and TypeScript libraries before node/relay builds. The existing Java
snapshot version was retained. No database migration is required because all new persisted enum values are appended.

## Both posting headings in reference instants

The requested text includes the referenced local posting and the referring remote posting. Keep grouping and store
the source posting snapshot in each substory, then rebuild the latest two source summaries with the group's target.
The notification carries the source metadata in separate fields after `remotePostingId`: owner name, full name,
source URI, gender, heading, sheriffs, and sheriff marks. The receiver creates the source summary and passes it through
the persistent avatar job and the sharing-added liberin.
The local posting remains `summaryData.posting`; `summaryData.references` lists the referring postings, newest first.
Display the source heading only for a single reference: "Alice mentioned your post ... in her post ...".
For multiple references, display "Alice and Bob mentioned your post ... in their posts", with only the original heading.
Source heading moderation applies only when that heading is displayed.
No conversion of existing reference instants is required.

- [x] Test outgoing source metadata and rendering of both headings before implementing the extension.
- [x] Extend the notification and summary schemas, regenerate consumers, and rebuild the Java library.
- [x] Carry source metadata through completion into substories; test persistence, regrouping, and API/event payloads.
- [x] Extend the canonical summary and four locales, including grouped wording and source moderation metadata.
- [x] Import relay presentation, synchronize its locales, run relevant suites/builds, and update federation docs.

Extension verification: the outgoing-notification regression and seven client cases first failed for the missing source
heading. The final full node suite passed all 500 tests; all 20 sharing integration tests passed against an isolated
PostgreSQL database, including source-heading persistence and target/heading restoration after unsharing.
All 369 client tests and the production build passed. Java moeralib passed 76 tests and installed successfully;
TypeScript moeralib passed 10 tests. Relay compilation and a schema/rendering check passed for all four languages,
including both headings, the source posting target, and the existing `ms_repeat` icon. No Android changes are needed.
The code review reported no important findings. Locale synchronization remains local to the client and relay.

Grouped wording refinement: source headings are substituted only in the singular ICU branch in all four locales.
Regression tests cover groups of two and four, the exact Russian wording, and moderation of displayed headings only.
All 378 client tests passed. Relay compilation and rendering checks passed for one, two, and four references in
English, Polish, Russian, and Ukrainian.

Notification field refinement: `POSTING_SHARED` carries the source metadata as individual fields, and the receiver
constructs `StorySummaryEntry` for the instant workflow. The outgoing JSON regression failed before the change and
passed afterwards; the receiving test deserializes the new payload and checks all metadata in the durable job.
The regenerated Java library passed 76 tests and installed successfully. All 520 node tests passed, including the
20 sharing integration tests on a fresh isolated PostgreSQL database. Regeneration left client and relay files
unchanged.
