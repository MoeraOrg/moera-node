# Node Source URI Propagation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an optional 1024-character source URI beside every node full name in the Node API, persisted node data,
notifications, instants, generated Moera libraries, and `moera-search`.

**Architecture:** Treat a source URI as historical identity metadata captured from the same profile snapshot as its full
name. Add mechanically named optional fields to the API, nullable shadow columns to persisted full-name copies, and
parallel parameters through existing conversion and event pipelines. Store the local value in `profile.source-uri` and
use a new notification fingerprint version when signing it.

**Tech Stack:** Java 21, Spring Boot, JPA/Hibernate, PostgreSQL/Flyway, YAML API schema and generators, Maven, Neo4j,
Python and TypeScript generated libraries.

**Spec:** `doc/plans/node-source-uri-design.md`

## Global Constraints

- `ProfileAttributes`, `ProfileInfo`, and `WhoAmI` use the field name `sourceUri`.
- Every other `*FullName` API field gets a field formed by replacing `FullName` with `SourceUri`; plain `fullName` gets
  `nodeSourceUri`.
- Every source URI is optional, has a maximum length of 1024 characters, and has no syntax validation.
- Persisted source URI columns are nullable `varchar(1024)` and old rows are not backfilled.
- A copied source URI must come from the same profile snapshot as its paired full name.
- Do not edit `doc/create_tables.sql`.
- Do not change moeralib snapshot versions.
- Preserve all pre-existing uncommitted changes in sibling repositories.
- Do not leave changes in `moera-client-react`, `moera-client-android`, or other client application repositories.
- Follow `AGENTS.md`: JPA queries remain explicit, accessors contain only value copying, and instant creation stays in
  dedicated `InstantsCreator` components invoked by liberin receptors.

---

### Task 1: Extend the public API and regenerate all language libraries

**Files:**
- Modify: `../moeraorg.github.io/_data/node_api.yml`
- Modify: `../moeraorg.github.io/_data/node_api_fingerprints.yml`
- Modify: `../moeraorg.github.io/_data/notifications.yml`
- Modify: `../moeraorg.github.io/_data/events.yml`
- Generated: `../java-moeralib/src/main/java/org/moera/lib/node/types/**`
- Generated: `../java-moeralib/src/main/java/org/moera/lib/node/types/notifications/**`
- Generated: `../java-moeralib/src/main/java/org/moera/lib/node/Fingerprints.java`
- Modify: `../java-moeralib/src/main/java/org/moera/lib/node/types/notifications/Notification.java`
- Generated: `../python-moeralib/src/moeralib/node/**`
- Generated: `../typescript-moeralib/src/node/**`
- Generated: `../moeraorg.github.io/_data/py_node_api.yml`
- Generated: `../moeraorg.github.io/_data/ts_node_api.yml`

**Interfaces:**
- Produces: optional Java/Python/TypeScript properties matching the field table below and
  `Fingerprints.notificationPacket2(String id, String nodeName, String fullName, String nodeSourceUri,
  Timestamp createdAt, String type, String notification)`.

- [ ] **Step 1: Capture dirty state and assert client-generated paths are initially clean**

Run:

```bash
git -C ../moeraorg.github.io status --short
git -C ../java-moeralib status --short
git -C ../python-moeralib status --short
git -C ../typescript-moeralib status --short
git -C ../moera-client-react status --short -- src/api
git -C ../moera-fcm-relay status --short
```

Expected: retain the known schema-description, version, validation, and generated documentation edits; client API and
FCM relay paths are clean before generation.

- [ ] **Step 2: Add every optional API field to the source schema**

Insert each field immediately after its paired full name, preserving object and field ordering. Use this exact shape,
with the corresponding name and error-code prefix substituted. Derive the error code from the paired full-name error by
replacing `full-name` with `source-uri`:

```yaml
      - type: String
        name: ownerSourceUri
        optional: true
        description: URI of the source of the node owner's identity
        constraints:
          - length:
              max: 1024
              error: posting.owner-source-uri.wrong-size
```

Apply this mapping to the 40 direct schema fields:

```text
ActivityReactionInfo.remoteSourceUri
CommentInfo.ownerSourceUri
CommentSourceText.ownerSourceUri
CommentText.ownerSourceUri
ContactInfo.nodeSourceUri
DraftInfo.ownerSourceUri
DraftText.ownerSourceUri
NotificationPacket.nodeSourceUri
PostingInfo.receiverSourceUri, ownerSourceUri
PostingSourceInfo.nodeSourceUri
PostingSourceText.ownerSourceUri
PostingText.ownerSourceUri
ProfileAttributes.sourceUri
ProfileInfo.sourceUri
ReactionDescription.ownerSourceUri
ReactionInfo.ownerSourceUri
RecommendedPostingInfo.ownerSourceUri
RecommendedNodeInfo.nodeSourceUri
RepliedTo.sourceUri
SearchEntryInfo.ownerSourceUri
SearchNodeInfo.nodeSourceUri
SearchRepliedTo.sourceUri
SheriffComplaintGroupInfo.remoteNodeSourceUri, remotePostingOwnerSourceUri, remoteCommentOwnerSourceUri
SheriffComplaintInfo.ownerSourceUri
SheriffComplaintText.ownerSourceUri, nodeSourceUri, postingOwnerSourceUri, commentOwnerSourceUri
SheriffOrderInfo.nodeSourceUri, postingOwnerSourceUri, commentOwnerSourceUri
StoryInfo.summarySourceUri, remoteSourceUri
StorySummaryEntry.ownerSourceUri
StorySummaryNode.ownerSourceUri
StorySummaryReaction.ownerSourceUri
WhoAmI.sourceUri
```

In `_data/notifications.yml`, add counterparts to all 21 full-name occurrences. These include `ownerSourceUri`,
`postingSourceUri`, `parentPostingSourceUri`, `postingOwnerSourceUri`, and `commentOwnerSourceUri`. In
`_data/events.yml`, add `nodeSourceUri` to `NODE_NAME_CHANGED`, plus `summarySourceUri` and `remoteSourceUri` to both
story event shapes. Apply the same optional-string, maximum-1024 constraint to these fields.

The Java notification base class is handwritten rather than generated from those 21 payload definitions. Add nullable
`senderSourceUri` storage and plain getter/setter beside `senderFullName` in `Notification.java`.

- [ ] **Step 3: Define notification packet fingerprint version 2**

Add a version based on version 1 with `nodeSourceUri` immediately after `fullName`:

```yaml
  - name: NotificationPacket
    versions:
      - version: 2
        fingerprint:
          - type: String
            field: objectType
            comment: <code>NOTIFICATION_PACKET</code>
          - type: String
            field: id
          - type: String
            field: nodeName
          - type: String
            field: fullName
          - type: String
            field: nodeSourceUri
          - type: timestamp
            field: createdAt
          - type: String
            field: type
          - type: String
            field: notification
```

Retain versions 0 and 1 unchanged.

- [ ] **Step 4: Run generation exactly once and remove out-of-scope generated diffs**

Run from `moera-node`:

```bash
../update-api
```

Review client/relay diffs, then restore only paths that were clean in Step 1:

```bash
git -C ../moera-client-react restore -- src/api
git -C ../moera-fcm-relay restore -- .
```

Expected: source schema, docs, Java/Python/TypeScript moeralibs change; no client or FCM relay diff remains.

- [ ] **Step 5: Verify generated contracts and validation**

Run:

```bash
rg -n 'SourceUri|source_uri' ../java-moeralib/src/main/java/org/moera/lib/node/types \
  ../python-moeralib/src/moeralib/node ../typescript-moeralib/src/node
rg -n 'maxSize\(.*SourceUri, 1024|max_length=1024|maxLength: 1024' \
  ../java-moeralib/src/main/java/org/moera/lib/node/types \
  ../python-moeralib/src/moeralib/node ../typescript-moeralib/src/node
```

Expected: all mapped fields exist and generated validators impose only maximum length 1024.

- [ ] **Step 6: Build and commit the generated contracts**

Run:

```bash
mvn -f ../java-moeralib/pom.xml clean install
python -m compileall -q ../python-moeralib/src/moeralib
npm --prefix ../typescript-moeralib test
```

Commit generated paths that do not overlap pre-existing user edits. Leave the existing `node_api.yml`, Java
`MoeraNode.java`/`ValidationUtil.java`/`pom.xml`, Python `node.py`, and TypeScript `node.ts` edits unstaged unless a
source-URI hunk in the same file can be staged separately and verified with `git diff --cached`:

```bash
git -C ../moeraorg.github.io add _data/node_api_fingerprints.yml _data/notifications.yml _data/events.yml \
  _data/py_node_api.yml _data/ts_node_api.yml
git -C ../java-moeralib add src/main/java/org/moera/lib/node/Fingerprints.java \
  src/main/java/org/moera/lib/node/types
git -C ../java-moeralib commit -m "Generate node source URI API fields"
git -C ../python-moeralib add src/moeralib/node/types.py src/moeralib/node/schemas.py \
  src/moeralib/node/fingerprints.py
git -C ../python-moeralib commit -m "Generate node source URI API fields"
git -C ../typescript-moeralib add src/node/types.ts src/node/schemas.mjs src/node/validators.js \
  src/node/validators.d.ts src/node/fingerprints.ts
git -C ../typescript-moeralib commit -m "Generate node source URI API fields"
```

Keep `node_api.yml` uncommitted with its preserved user edit unless exact hunk staging is practical; correctness of the
working tree takes precedence over making a mixed commit.

### Task 2: Store and return the local profile source URI

**Files:**
- Modify: `src/main/resources/options.yaml`
- Modify: `src/main/java/org/moera/node/global/RequestContext.java`
- Modify: `src/main/java/org/moera/node/global/RequestContextImpl.java`
- Modify: `src/main/java/org/moera/node/global/UniversalContext.java`
- Modify: `src/main/java/org/moera/node/task/Task.java`
- Modify: `src/main/java/org/moera/node/model/ProfileAttributesUtil.java`
- Modify: `src/main/java/org/moera/node/model/ProfileInfoUtil.java`
- Modify: `src/main/java/org/moera/node/model/WhoAmIiUtil.java`
- Test: `src/test/java/org/moera/node/model/ProfileSourceUriTest.java`

**Interfaces:**
- Produces: `RequestContext.sourceUri()`, `UniversalContext.sourceUri()`, `Task.sourceUri()`; option
  `profile.source-uri`; API output through `ProfileInfo.sourceUri` and `WhoAmI.sourceUri`.

- [ ] **Step 1: Write a failing profile conversion test**

Create `ProfileSourceUriTest` using mocked option collaborators and a real `Options`. The core assertions are:

```java
ProfileAttributes attributes = new ProfileAttributes();
attributes.setSourceUri("https://example.org/alice");
ProfileAttributesUtil.toOptions(attributes, options, textConverter);

Assertions.assertEquals("https://example.org/alice", options.getString("profile.source-uri"));
Assertions.assertEquals("https://example.org/alice", ProfileInfoUtil.build(options).getSourceUri());
```

Add a second test that passes `attributes.setSourceUri("")` and asserts the option and returned field are `null`.

- [ ] **Step 2: Run the test and verify RED**

Run: `./mvnw -Dtest=ProfileSourceUriTest test`

Expected: compilation or assertion failure because the option and conversion path do not exist.

- [ ] **Step 3: Implement local storage and context access**

Add this option descriptor next to `profile.full-name`:

```yaml
- name: profile.source-uri
  type: string
```

Add pure accessors following the full-name pattern:

```java
public String sourceUri() {
    return options != null ? options.getString("profile.source-uri") : null;
}
```

Write/reset it in `ProfileAttributesUtil`; set it in both `ProfileInfoUtil.build` variants and `WhoAmIiUtil`.

- [ ] **Step 4: Verify GREEN and compile callers**

Run:

```bash
./mvnw -Dtest=ProfileSourceUriTest test
./mvnw -q -DskipTests compile
```

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/options.yaml src/main/java/org/moera/node/global \
  src/main/java/org/moera/node/task/Task.java src/main/java/org/moera/node/model/ProfileAttributesUtil.java \
  src/main/java/org/moera/node/model/ProfileInfoUtil.java src/main/java/org/moera/node/model/WhoAmIiUtil.java \
  src/test/java/org/moera/node/model/ProfileSourceUriTest.java
git commit -m "Store node profile source URI"
```

### Task 3: Add nullable source URI persistence fields

**Files:**
- Create: `src/main/resources/db/migration/V307__node_source_uri.sql`
- Modify: `src/main/java/org/moera/node/data/{Contact,Draft,Entry,EntrySource,OwnComment,OwnPosting,OwnReaction,Reaction,Story}.java`
- Modify: `src/main/java/org/moera/node/data/{SheriffComplaint,SheriffComplaintGroup,SheriffOrder}.java`
- Modify: `src/main/java/org/moera/node/data/ContactRepository.java`
- Test: `src/test/java/org/moera/node/data/SourceUriPersistenceModelTest.java`

**Interfaces:**
- Produces: nullable entity properties named by replacing each persisted `FullName` suffix with `SourceUri`.

- [ ] **Step 1: Write a failing reflection test for the persistence invariant**

```java
private static final List<Class<?>> ENTITIES = List.of(
    Contact.class, Draft.class, Entry.class, EntrySource.class, OwnComment.class, OwnPosting.class,
    OwnReaction.class, Reaction.class, SheriffComplaint.class, SheriffComplaintGroup.class,
    SheriffOrder.class, Story.class
);

@Test
void everyPersistedFullNameHasSourceUri() throws Exception {
    for (Class<?> entity : ENTITIES) {
        for (Field field : entity.getDeclaredFields()) {
            if (field.getName().endsWith("FullName")) {
                String source = field.getName().substring(0, field.getName().length() - 8) + "SourceUri";
                Field sourceField = entity.getDeclaredField(source);
                Assertions.assertEquals(1024, sourceField.getAnnotation(Size.class).max());
            }
        }
    }
}
```

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -Dtest=SourceUriPersistenceModelTest test`

Expected: `NoSuchFieldException` for `remoteSourceUri` in `Contact`.

- [ ] **Step 3: Add the entity fields and pure accessors**

Add `@Size(max = 1024) private String ...SourceUri;` beside each full name. The exact entity inventory is:

```text
Contact.remoteSourceUri
Draft.ownerSourceUri
Entry.receiverSourceUri, ownerSourceUri, repliedToSourceUri
EntrySource.remoteSourceUri
OwnComment.remoteSourceUri, remoteRepliedToSourceUri
OwnPosting.remoteSourceUri
OwnReaction.remoteSourceUri
Reaction.ownerSourceUri
SheriffComplaint.ownerSourceUri
SheriffComplaintGroup.remoteNodeSourceUri, remotePostingOwnerSourceUri, remoteCommentOwnerSourceUri
SheriffOrder.remoteNodeSourceUri, remotePostingOwnerSourceUri, remoteCommentOwnerSourceUri
Story.remoteSourceUri, remotePostingSourceUri, remoteOwnerSourceUri
```

Extend `SheriffOrder.setRemotePosting` and `setRemoteComment` to copy the URI beside the full name. Extend the explicit
`ContactRepository.updateRemoteDetails` JPQL assignment and method parameter with `remoteSourceUri`.

- [ ] **Step 4: Add the Flyway migration**

Create one `ALTER TABLE ... ADD COLUMN ... varchar(1024);` statement for every field above. Use snake case and the table
names already declared on the entities. Do not add defaults or `NOT NULL` constraints.

- [ ] **Step 5: Verify GREEN**

Run:

```bash
./mvnw -Dtest=SourceUriPersistenceModelTest test
./mvnw -q -DskipTests compile
```

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/db/migration/V307__node_source_uri.sql src/main/java/org/moera/node/data \
  src/test/java/org/moera/node/data/SourceUriPersistenceModelTest.java
git commit -m "Persist source URI with copied node names"
```

### Task 4: Propagate source URIs through core entry and contact models

**Files:**
- Modify: `src/main/java/org/moera/node/model/{ActivityReactionInfoUtil,CommentInfoUtil,CommentTextUtil,ContactInfoUtil}.java`
- Modify: `src/main/java/org/moera/node/model/{DraftInfoUtil,DraftTextUtil,PostingInfoUtil,PostingSourceInfoUtil}.java`
- Modify: `src/main/java/org/moera/node/model/{PostingSourceTextUtil,PostingTextUtil,ReactionDescriptionUtil,ReactionInfoUtil}.java`
- Modify: `src/main/java/org/moera/node/model/{RepliedToUtil,SearchNodeInfoUtil}.java`
- Modify: `src/main/java/org/moera/node/operations/{PostingOperations,CommentOperations,ContactOperations,ContactSearch}.java`
- Modify: `src/main/java/org/moera/node/picker/Picker.java`
- Modify: `src/main/java/org/moera/node/rest/DraftController.java`
- Modify: `src/main/java/org/moera/node/rest/task/{RemotePostingPostJob,RemoteCommentPostJob}.java`
- Modify: `src/main/java/org/moera/node/rest/task/{RemotePostingReactionPostJob,RemoteCommentReactionPostJob}.java`
- Modify: `src/main/java/org/moera/node/rest/task/upgrade/{AllContactDetailsDownloadTask,ContactsUpgradeTask}.java`
- Test: `src/test/java/org/moera/node/model/SourceUriConversionTest.java`

**Interfaces:**
- Consumes: generated API fields from Task 1 and entity fields/context accessors from Tasks 2–3.
- Produces: lossless URI round trips for posting, comment, draft, reaction, replied-to, posting-source, and contact data.

- [ ] **Step 1: Write failing conversion tests**

Cover at least one writable and one readable path for each entity family. Example:

```java
PostingText text = new PostingText();
text.setOwnerFullName("Alice");
text.setOwnerSourceUri("https://example.org/alice");
Entry entry = new Entry();
PostingTextUtil.toEntry(text, entry);
Assertions.assertEquals("https://example.org/alice", entry.getOwnerSourceUri());
Assertions.assertTrue(PostingTextUtil.sameAsEntry(text, entry));
```

Also assert `CommentText -> Entry`, `DraftText -> Draft`, `ReactionDescription -> Reaction`, `Entry -> PostingInfo`,
`Entry -> CommentInfo`, `EntrySource -> PostingSourceInfo`, `Entry.repliedTo -> RepliedTo`, and
`Contact -> ContactInfo/SearchNodeInfo`.

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -Dtest=SourceUriConversionTest test`

Expected: assertions return `null` or equality checks fail because conversion helpers omit URI fields.

- [ ] **Step 3: Add parallel copies in every conversion helper**

For every existing full-name assignment add its URI assignment from the same object, for example:

```java
info.setOwnerFullName(entry.getOwnerFullName());
info.setOwnerSourceUri(entry.getOwnerSourceUri());
```

Extend helper signatures such as `PostingSourceTextUtil.build`, `ReactionDescriptionUtil.build`, and story-summary
builders by placing the URI immediately after the paired full-name argument.

- [ ] **Step 4: Extend capture points and explicit contact updates**

Local creation uses `requestContext.sourceUri()` or `universalContext.sourceUri()`. Remote jobs copy `getSourceUri()`,
`getOwnerSourceUri()`, or `getNodeSourceUri()` from the same fetched object used for the full name. Update
`ContactOperations` and upgrade tasks so a URI-only profile change is persisted and considered present.

- [ ] **Step 5: Verify GREEN**

Run:

```bash
./mvnw -Dtest=SourceUriConversionTest test
./mvnw -q -DskipTests compile
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/org/moera/node/model src/main/java/org/moera/node/operations \
  src/main/java/org/moera/node/picker src/main/java/org/moera/node/rest/DraftController.java \
  src/main/java/org/moera/node/rest/task src/test/java/org/moera/node/model/SourceUriConversionTest.java
git commit -m "Propagate source URI through entry models"
```

### Task 5: Version notification packet signatures and sender metadata

**Files:**
- Modify: `src/main/java/org/moera/node/fingerprint/NotificationPacketFingerprintBuilder.java`
- Modify: `src/main/java/org/moera/node/notification/send/NotificationSender.java`
- Modify: `src/main/java/org/moera/node/rest/NotificationController.java`
- Modify: `src/main/java/org/moera/node/notification/receive/DefrostNodeJob.java`
- Test: `src/test/java/org/moera/node/fingerprint/NotificationPacketFingerprintBuilderTest.java`

**Interfaces:**
- Consumes: `Fingerprints.notificationPacket2` and `NotificationPacket.nodeSourceUri`.
- Produces: `LATEST_VERSION = 2`; versions 0, 1, and 2 remain verifiable.

- [ ] **Step 1: Write failing fingerprint tests**

```java
NotificationPacket packet = packet();
packet.setNodeSourceUri("https://example.org/alice");
byte[] first = NotificationPacketFingerprintBuilder.build((short) 2, packet);
packet.setNodeSourceUri("https://example.org/bob");
byte[] second = NotificationPacketFingerprintBuilder.build((short) 2, packet);
Assertions.assertFalse(Arrays.equals(first, second));
Assertions.assertDoesNotThrow(() -> NotificationPacketFingerprintBuilder.build((short) 1, packet));
```

Also assert version 1 output is unchanged by `nodeSourceUri`.

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -Dtest=NotificationPacketFingerprintBuilderTest test`

Expected: unknown fingerprint version 2.

- [ ] **Step 3: Implement version 2 and packet propagation**

Set `LATEST_VERSION = 2` and add:

```java
case 2 -> Fingerprints.notificationPacket2(
    packet.getId(), packet.getNodeName(), packet.getFullName(), packet.getNodeSourceUri(),
    Util.toTimestamp(packet.getCreatedAt()), packet.getType(), packet.getNotification()
);
```

Set `packet.setNodeSourceUri(sourceUri())` in the sender. Add `senderSourceUri` to the internal received-notification
model if it has a paired `senderFullName`, and copy `packet.getNodeSourceUri()` in normal and defrost receive paths.

- [ ] **Step 4: Verify GREEN**

Run: `./mvnw -Dtest=NotificationPacketFingerprintBuilderTest test`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/org/moera/node/fingerprint src/main/java/org/moera/node/notification \
  src/main/java/org/moera/node/rest/NotificationController.java \
  src/test/java/org/moera/node/fingerprint/NotificationPacketFingerprintBuilderTest.java
git commit -m "Sign source URI in notification packets"
```

### Task 6: Propagate source URIs through notifications, liberins, events, and instants

**Files:**
- Modify: every non-sheriff matching file under `src/main/java/org/moera/node/model/notification/`
- Modify: every non-sheriff matching file under `src/main/java/org/moera/node/rest/notification/`
- Modify: every non-sheriff matching file under `src/main/java/org/moera/node/liberin/model/`
- Modify: matching non-sheriff receptors under `src/main/java/org/moera/node/liberin/receptor/`
- Modify: `src/main/java/org/moera/node/model/event/{NodeNameChangedEvent,RemoteNodeFullNameChangedEvent,StoryEvent}.java`
- Modify: all non-sheriff `*Instants.java` files that currently read or write a full name
- Modify: `src/main/java/org/moera/node/model/{StoryInfoUtil,StorySummaryEntryUtil,StorySummaryNodeUtil,StorySummaryReactionUtil}.java`
- Test: `src/test/java/org/moera/node/model/StorySourceUriTest.java`
- Test: `src/test/java/org/moera/node/liberin/model/LiberinSourceUriTest.java`

**Interfaces:**
- Produces: matching `*SourceUri` constructor arguments, accessors, JSON model keys, notification properties, story
  entity fields, `StoryInfo` fields, and summary fields.

- [ ] **Step 1: Write failing story and liberin tests**

Test entity-to-API and JSON-backed propagation:

```java
Story story = new Story();
story.setId(UUID.randomUUID());
story.setStoryType(StoryType.POSTING_UPDATE_TASK_FAILED);
story.setRemoteFullName("Alice");
story.setRemoteSourceUri("https://example.org/alice");
StoryInfo info = StoryInfoUtil.build(story, false, ignored -> null, null);
Assertions.assertEquals("https://example.org/alice", info.getRemoteSourceUri());
```

For a representative multi-owner liberin, serialize its `getModel()` and assert both `postingOwnerSourceUri` and
`commentOwnerSourceUri` equal the constructor inputs.

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -Dtest=StorySourceUriTest,LiberinSourceUriTest test`

Expected: missing constructor/accessor or `null` output.

- [ ] **Step 3: Extend transient models and processor chains mechanically**

For every non-sheriff `*FullName` field in notification utils, processors, liberins, and events, add the corresponding URI field,
getter/setter, constructor parameter, and JSON model property. The important composite pairs include:

```text
postingFullName/postingSourceUri
parentPostingFullName/parentPostingSourceUri
reactionFullName/reactionSourceUri
postingOwnerFullName/postingOwnerSourceUri
commentOwnerFullName/commentOwnerSourceUri
remoteFullName/remoteSourceUri
```

Pass each URI through receptors to its dedicated instant creator. Do not persist stories directly in receptors.
Sheriff-specific chains are completed in Task 7.

- [ ] **Step 4: Extend instant and summary persistence**

Whenever an instant assigns `Story.remoteFullName`, `remotePostingFullName`, or `remoteOwnerFullName`, assign the paired
URI from the same input. Preserve URI fields when grouping substories and rebuilding summaries. Extend
`StorySummary*Util.build` signatures with the URI directly after full name.

- [ ] **Step 5: Update profile-change events**

Include local `sourceUri` in `NodeNameChangedLiberin`/event. Extend remote profile update and blocking jobs so
`RemoteNodeFullNameChangedLiberin` carries `sourceUri`, and make the receptor publish a changed event when either value
changes.

- [ ] **Step 6: Verify GREEN and audit parity**

Run:

```bash
./mvnw -Dtest=StorySourceUriTest,LiberinSourceUriTest test
./mvnw -q -DskipTests compile
rg -l 'FullName' src/main/java/org/moera/node/instant src/main/java/org/moera/node/liberin \
  src/main/java/org/moera/node/model/notification src/main/java/org/moera/node/rest/notification
```

Inspect every listed file and confirm each transported full-name value has its paired URI.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/org/moera/node/instant src/main/java/org/moera/node/liberin \
  src/main/java/org/moera/node/model/event src/main/java/org/moera/node/model/notification \
  src/main/java/org/moera/node/rest/notification src/main/java/org/moera/node/model/StoryInfoUtil.java \
  src/main/java/org/moera/node/model/StorySummary*Util.java src/test/java/org/moera/node/model/StorySourceUriTest.java \
  src/test/java/org/moera/node/liberin/model/LiberinSourceUriTest.java
git commit -m "Carry source URI through notifications and instants"
```

### Task 7: Propagate sheriff source URI fields

**Files:**
- Modify: `src/main/java/org/moera/node/model/{SheriffComplaintTextUtil,SheriffComplaintInfoUtil}.java`
- Modify: `src/main/java/org/moera/node/model/{SheriffComplaintGroupInfoUtil,SheriffOrderInfoUtil}.java`
- Modify: `src/main/java/org/moera/node/rest/task/{SheriffComplaintGroupPrepareJob,SheriffOrderPostJob}.java`
- Modify: sheriff notification/liberin/receptor/instant files found in Task 6's parity audit
- Test: `src/test/java/org/moera/node/model/SheriffSourceUriTest.java`

**Interfaces:**
- Produces: round trips for complaint owner, remote node, posting owner, and comment owner source URIs.

- [ ] **Step 1: Write failing sheriff conversion tests**

```java
SheriffComplaintText text = new SheriffComplaintText();
text.setOwnerSourceUri("https://example.org/reporter");
SheriffComplaint complaint = new SheriffComplaint();
SheriffComplaintTextUtil.toSheriffComplaint(text, complaint);
Assertions.assertEquals("https://example.org/reporter", complaint.getOwnerSourceUri());
```

Add equivalent assertions for group info and order info containing all three composite remote URI fields.

- [ ] **Step 2: Run and verify RED**

Run: `./mvnw -Dtest=SheriffSourceUriTest test`

- [ ] **Step 3: Implement sheriff capture and output**

Copy each source URI beside its paired full name in converters, preparation/post jobs, notification utilities, liberins,
receptors, and sheriff instant summaries. When `SheriffOrder.setRemotePosting` or `setRemoteComment` is used, rely on
the entity helper added in Task 3 rather than duplicating assignments.

- [ ] **Step 4: Verify GREEN and commit**

Run:

```bash
./mvnw -Dtest=SheriffSourceUriTest test
./mvnw -q -DskipTests compile
git add src/main/java/org/moera/node/model/Sheriff* src/main/java/org/moera/node/rest/task/Sheriff* \
  src/main/java/org/moera/node/instant/SheriffInstants.java src/test/java/org/moera/node/model/SheriffSourceUriTest.java
git commit -m "Propagate source URI through sheriff records"
```

### Task 8: Store and return source URIs in Moera Search

**Files:**
- Modify: `../moera-search/src/main/java/org/moera/search/config/Config.java`
- Modify: `../moera-search/src/main/resources/application.yml`
- Modify: `../moera-search/src/main/java/org/moera/search/rest/{WhoAmIiController,NotificationController}.java`
- Modify: `../moera-search/src/main/java/org/moera/search/api/fingerprint/NotificationPacketFingerprintBuilder.java`
- Modify: `../moera-search/src/main/java/org/moera/search/api/model/{RecommendedNodeInfoUtil,SearchNodeInfoUtil,SearchRepliedToUtil}.java`
- Modify: `../moera-search/src/main/java/org/moera/search/data/{NodeRepository,PostingRepository,CommentRepository}.java`
- Modify: `../moera-search/src/main/java/org/moera/search/data/{ReactionRepository,EntryRepository}.java`
- Test: `../moera-search/src/test/java/org/moera/search/api/model/SourceUriModelTest.java`
- Test: `../moera-search/src/test/java/org/moera/search/api/fingerprint/NotificationPacketFingerprintBuilderTest.java`

**Interfaces:**
- Consumes: generated Java types and fingerprint version 2.
- Produces: Neo4j `sourceUri`/`ownerSourceUri` properties and populated source URI API results.

- [ ] **Step 1: Point the build at the locally installed generated moeralib**

Use the existing property override without editing `pom.xml`:

```bash
mvn -q -DskipTests -Dmoeralib.version=0.19.2-SNAPSHOT compile
```

Use the actual unchanged version from `../java-moeralib/pom.xml` if it differs because of the pre-existing user edit.

- [ ] **Step 2: Write failing model and fingerprint tests**

Test the pure result builders with Neo4j value objects or the repository's existing database test utility. Assert node
`sourceUri`, entry `ownerSourceUri` with node fallback, and `SearchRepliedTo.sourceUri`. Copy the Task 5 fingerprint
test and assert version 2 changes when `nodeSourceUri` changes while version 1 does not.

- [ ] **Step 3: Run and verify RED**

Run:

```bash
mvn -f ../moera-search/pom.xml -Dmoeralib.version=0.19.2-SNAPSHOT \
  -Dtest=SourceUriModelTest,NotificationPacketFingerprintBuilderTest test
```

- [ ] **Step 4: Extend configuration, ingestion, and query output**

Add plain `nodeSourceUri` configuration accessors and set `WhoAmI.sourceUri`. In Neo4j argument maps and `SET` clauses,
store:

```text
MoeraNode.sourceUri <- WhoAmI.sourceUri
Posting.ownerSourceUri <- PostingInfo.ownerSourceUri
Comment.ownerSourceUri <- CommentInfo.ownerSourceUri
Reaction.ownerSourceUri <- ReactionInfo.ownerSourceUri
```

Serialize `SearchRepliedTo.sourceUri`. In `EntryRepository.buildSearchResult`, read `entry.ownerSourceUri`; when it
is absent, fall back to `owner.sourceUri` in the same branch used for full-name fallback. Set URI fields in recommended
and node search result builders.

- [ ] **Step 5: Implement notification version 2 and sender metadata**

Mirror Task 5's switch exactly, set received sender source URI from `packet.getNodeSourceUri()`, and retain versions 0
and 1.

- [ ] **Step 6: Verify GREEN and commit**

Run:

```bash
mvn -f ../moera-search/pom.xml -Dmoeralib.version=0.19.2-SNAPSHOT \
  -Dtest=SourceUriModelTest,NotificationPacketFingerprintBuilderTest test
mvn -f ../moera-search/pom.xml -Dmoeralib.version=0.19.2-SNAPSHOT test
git -C ../moera-search add src
git -C ../moera-search commit -m "Index and return node source URIs"
```

### Task 9: Complete parity audit and full verification

**Files:**
- Modify: any in-scope handwritten file found by the audit with a transported `*FullName` lacking `*SourceUri`
- Verify only: all touched repositories

**Interfaces:**
- Produces: evidence that the naming, storage, generated-code, and repository-scope invariants hold.

- [ ] **Step 1: Audit public API parity**

Run:

```bash
rg -n 'name: [A-Za-z0-9]*[Ff]ullName' ../moeraorg.github.io/_data/node_api.yml
rg -n 'name: [A-Za-z0-9]*SourceUri|name: sourceUri' ../moeraorg.github.io/_data/node_api.yml
```

Pair every result manually using the contract table; `ProfileAttributes`, `ProfileInfo`, and `WhoAmI` must be the only
profile exceptions using plain `sourceUri`.

- [ ] **Step 2: Audit persisted and transported Java fields**

Run:

```bash
rg -l 'FullName' src/main/java ../moera-search/src/main/java | sort
rg -n 'FullName' src/main/java/org/moera/node/data
rg -n 'SourceUri' src/main/java/org/moera/node/data
```

Inspect every file from the first command. UI-only formatting code may consume a full name without a URI; any code that
constructs, stores, copies, returns, serializes, or publishes the full name must do the same for its URI.

- [ ] **Step 3: Run the full builds**

```bash
mvn -f ../java-moeralib/pom.xml clean install
./mvnw clean verify
mvn -f ../moera-search/pom.xml -Dmoeralib.version=0.19.2-SNAPSHOT clean verify
python -m compileall -q ../python-moeralib/src/moeralib
npm --prefix ../typescript-moeralib test
```

- [ ] **Step 4: Verify formatting, scope, and preserved user changes**

```bash
for repo in moera-node moeraorg.github.io java-moeralib python-moeralib typescript-moeralib moera-search; do
  git -C ../$repo diff --check
  git -C ../$repo status --short
done
git -C ../moera-client-react status --short -- src/api
git -C ../moera-client-android status --short
git -C ../moera-fcm-relay status --short
```

Expected: no whitespace errors; only the six in-scope repositories have task changes; the pre-existing user changes
remain present or are included in their original repository commits without alteration.

- [ ] **Step 5: Commit any audit fixes and record final evidence**

Stage only files changed to close a demonstrated parity gap, commit them in their owning repository with message
`Complete node source URI propagation`, then record the successful commands and changed-repository status in the final
handoff.
