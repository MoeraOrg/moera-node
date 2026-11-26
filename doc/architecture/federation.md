# Federation

[Architecture index](../architecture.md)

## Naming and trust

`NamingClient`/`NamingCache` resolve registered names to location and signing information.
[NodeApi](../../src/main/java/org/moera/node/api/node/NodeApi.java) constructs the remote moeralib client, optionally
with a carte. A downloaded copy is not independently verified merely because it was fetched: separate verification
workflows validate versioned fingerprints and may require the key valid at the object's timestamp. Incoming packets
use the sender's current naming record. Signed-field changes must preserve fingerprint-version compatibility.

## Notifications

Receptors submit typed notifications to
[NotificationSenderPool](../../src/main/java/org/moera/node/notification/send/NotificationSenderPool.java), which
filters recipients and normally stores one pending message per recipient. A sender drains each local-node/peer
direction serially; startup restores pending rows. Each network attempt creates a fresh signed packet, so packet ID
is not a durable deduplication key. Retry policy distinguishes transient connectivity/naming failures, fatal responses,
and stale subscriptions/leases; work is removed after delivery or abandonment.

[NotificationController](../../src/main/java/org/moera/node/rest/NotificationController.java) verifies freshness and
signature before trusting a handled message's sender, validates its payload, and routes it to an annotated processor.
Recognized unhandled types are acknowledged without processing. A frozen node stores verified packets for later replay
and expiry, returning `FROZEN`; the sender then considers that item delivered and pauses further work to reduce load.
`DefrostNodeJob` deletes replayed rows even on processing failure, so this is bounded buffering, not permanent recovery.

Mapped handlers must keep validation/local updates short. Delegate downloads and other long work to persistent jobs
with the local node ID. [Asynchronous processing](asynchronous-processing.md#commit-boundaries) defines atomic job
scheduling, transient liberins, and the gaps that remain. Lost replies can cause duplicate remote effects.

## Subscriptions and picking

`SubscriptionManager` uses the [local reference count](persistence.md#relationships-and-snapshots) to establish
pending remote registrations and close registrations after their last reference disappears. Visibility-setting changes
update existing registrations.
Feed seeding excludes this node's originals, duplicate original identities, and publications already in the destination
feed; a cached posting without that publication remains eligible.

The node selects feed recommendations locally from candidates supplied by the configured search service. For each
selected posting it submits a pick and reports acceptance to the service.
See [FetchRecommendationJob](../../src/main/java/org/moera/node/rest/task/FetchRecommendationJob.java).

[PickerPool](../../src/main/java/org/moera/node/picker/PickerPool.java) persists fetch/refresh intent and runs a
[Picker](../../src/main/java/org/moera/node/picker/Picker.java) per local-node/peer direction. Picks retain target,
provenance, optional publication attributes, recommendation intent, and parent/reference context across redirects and
retries. A complete original address targets that original while preserving the sending node as `EntrySource`.
Authoritative receiver and discovery source need not coincide.

Picker fetches remote state and [prepares media](media.md#remote-downloads), then locks/reloads the posting for final
publication. Changed snapshots or missing parents request retry instead of overwriting concurrent state. The cached
entry, local ownership/attachments, source, totals, reference resolution, and requested stories commit together.
Additional work follows successful publication. A fetch without a destination feed need not add a story.

An unchanged content revision can still require receiver-date, sharing-total, or recommendation updates. Absent
recommendation intent preserves local state; explicit intent applies even without an edit. Story permissions and source
references remain independent of downloaded entry permissions. Picks have bounded retries/expiry and are discarded for
absent local nodes; they are not permanent synchronization requests. Pick scheduling differs from atomic job scheduling
as documented in the asynchronous chapter.

Evidence: [PickerTest](../../src/test/java/org/moera/node/picker/PickerTest.java) and
[MediaDownloadTransactionsIT](../../src/test/java/org/moera/node/media/MediaDownloadTransactionsIT.java).

## Sharing flow

Timeline republications report to the node hosting the original posting. While that node can view the story, a change
in its feed position sends `POSTING_SHARING_UPDATED`. Gaining access sends `POSTING_SHARED` with the current position.
Losing access sends `POSTING_UNSHARED` with the previously visible position.

Links from original postings to other authors' nodes use a separate sharing type with the same
[identity rules](persistence.md#references-and-sharings). For these links and for mentions in postings and comments,
signing the source revision triggers direct notifications. The node compares the sets of targets in the current and
preceding signed revisions. It expands mentioned names first, so `alice` and `alice_0` identify one target, reported as
`alice_0`. Removing its last occurrence withdraws the mention. Deleting the entry withdraws its previously visible
mentions and links.

1. `PostingSharingProcessor` applies changes under the original posting lock. A move updates only an existing
   republication from the same sender and preserves its list position and total. A late move after removal is ignored.
   Creating a sharing updates the total and schedules its `PostingSharedJob` in the same transaction.
2. The job resolves avatar work outside the handler, then completes only a still-existing sharing under the lock.
3. The added liberin's receptor rechecks existence before creating the instant. Removal captures substory IDs for
   post-commit group rebuilding/deletion. Older added work cannot resurrect an already removed sharing.

List, total, and subscriber-counter notifications have separate viewing permissions. Picked copies receive totals,
not ownership of the original's list. Publication/contact-favor effects are local and do not depend on peer delivery.
See [PostingSharedJob](../../src/main/java/org/moera/node/rest/notification/PostingSharedJob.java) and
[PostingSharingProcessorTest](../../src/test/java/org/moera/node/rest/notification/PostingSharingProcessorTest.java).

## Search subscribers

Moera Search owns the index and receives content, profile, reaction, and media-text updates through the peer pipeline.
The [notification schema](../../../moeraorg.github.io/_data/notifications.yml) defines the payloads.
Deploy support for new update types to search receivers first. A receiver that rejects an unknown type can delay
subsequent notifications to that peer during retries.

Visibility and deletion context must survive entity removal. Posting deletion suppresses redundant story-removal
updates, while standalone removal from a public feed has its own search update. Historical identity snapshots and
remote media node/ID pairs must be preserved.
