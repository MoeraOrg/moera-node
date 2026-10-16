# Federation

## Locating and calling nodes

`NamingClient` talks to the Moera naming service. `NamingCache` caches registered-name details, including the current
node URI and signing key. `NodeApi` resolves a remote registered name through this cache and creates a generated
`MoeraNode` client over OkHttp. Calls may be anonymous or carry a carte and authenticated mode.

Remote writes are generally performed by jobs under `rest.task`. A job retains enough parameters and state to retry a
multi-step exchange without keeping an HTTP request open. Remote verification tasks fetch signed objects again and
record whether a posting, comment, or reaction still matches its claimed fingerprint.

`SubscriptionTask` creates the subscriber record on the remote node. Its visibility follows the subscribing node's
`subscriptions.view` setting: `public` and `signed` are preserved, while all other principals become `private`
on the remote node so the subscribing node retains access. Changes to this setting propagate to existing remote feed
subscriber records through `AllRemoteSubscribersUpdateJob` with the same principal mapping. The request structures are
defined in the [Node API](https://moera.org/development/api/node/).

## Outgoing notifications

Liberin receptors construct typed notification objects and send them through `NotificationSenderPool`. Directions can
target one node, subscribers to a feed or entry, members of a friend group, or holders of a media lease. Principal
filters are evaluated before a notification is queued for a recipient.

Each outbound notification is serialized into `pending_notifications` before delivery. A
`NotificationSender` is serialized per `(local node ID, remote node name)` direction, which preserves order to one
peer while allowing unrelated peers to progress concurrently. Pending rows are restored after domains are configured.

The sender wraps the notification in a `NotificationPacket` containing a unique ID, local profile snapshot, creation
time, notification type, and JSON payload. It signs a versioned packet fingerprint with the local node's signing key.
Delivery uses exponential delay from 30 seconds to six hours, normally for up to seven days. Naming failures have a
shorter overall retry period, and permanent authentication, validation, or not-found failures stop delivery.

The receiver's `FROZEN` result records the peer as frozen and delays further sends. Connectivity state is retained so a
known failing or frozen peer does not consume sender capacity as aggressively as a healthy peer.

## Incoming notifications

`POST /moera/api/notifications` validates the packet type and freshness, resolves the sender's current signing key, and
verifies the fingerprint before deserializing the typed notification. `NotificationRouter` maps each notification type
to one method on an `@NotificationProcessor` component.

If the local node is frozen, the original packet is stored in `frozen_notifications` for later processing. Otherwise,
sender name, full name, source URI, gender, and avatar are attached to the notification before its processor runs.
Processors may update small state synchronously or enqueue a persistent after-commit job.

Versioned fingerprints preserve compatibility. The current notification signature covers the source URI as well as
the sender's full name; receivers can still verify older packet versions that did not include it.

## Picking remote content

`RemoteFeedFetchJob` seeds a subscription's destination feed from a remote feed. Existing postings by the remote
author in the destination feed count toward the fetch limit. Subscription creation clears their recommendation flag
before scheduling the job. The job reads existing postings in a short read transaction, then requests the remote feed
outside that transaction using the remaining limit. Candidates whose original node is the local node are skipped.
Other candidates already published in the destination feed are skipped using their original node and posting ID,
including reposts; cached remote postings without a publication in that feed remain eligible for picking. The job
continues through older slices until enough distinct eligible postings are selected or the remote feed is exhausted,
then submits picks from oldest to newest. Skipped stories and duplicate reposts do not consume the remaining limit.

A `Pick` is durable work to fetch or refresh a remote posting and its related data. `PickerPool` restores pending picks
at startup and runs one `Picker` for each `(local node ID, remote node name)` direction. This prevents concurrent
fetches from the same peer from stepping on each other's posting state.

A pick's nullable recommendation flag records whether to change the cached posting's recommendation status. A null
value preserves the status; explicit true or false values apply even when the posting is already cached and its
revision is unchanged. New cached postings are not recommended unless the pick explicitly requests it. Redirects,
retries, and process restarts preserve this intent.

A picker retries recoverable failures with an increasing delay from 30 seconds up to six hours. The final write
transaction locks and reloads the local posting, revalidates the earlier snapshot, and commits the entry, revision,
attachments, permissions, reaction totals, and sources together. Liberins and child picks follow the successful
commit.

Picks expire six hours after creation. Startup restoration, scheduling, and the ten-second retry scan discard picks
that have expired or refer to a local node absent from `Domains`. This also removes picks left marked as running after
a picker stops before processing its queue. Not-found failures discard the affected pick immediately. Failure updates
affect only existing durable picks, so a late failure cannot recreate a discarded row.

The remote-posting republish API creates one durable pick for each requested feed publication. Each pick retains the
remote source feed name, local destination feed name, publication time, external source references, and pinned, viewed,
read, and satisfied flags, so retries, redirects, and process restarts preserve the publication intent. The external
source references belong to the resulting story rather than to the downloaded posting. After downloading or refreshing
the posting, the picker adds a new story for the requested publication. A publication without an explicit local feed
name downloads the posting without adding a story.

Remote attachments initially use `RemoteMediaFile` metadata and an optional lease. A later download may add a local
`MediaFileOwner` to the same attachment. The remote record retains origin identity and metadata needed for federation,
while the local owner supplies bytes and local access control.

## Sharing notifications

Publishing a picked posting in `timeline` sends `POSTING_SHARED` to the original owner's node. Removing that
publication, including moving it to another feed, sends `POSTING_UNSHARED`. Both notifications are delivered only if
the owner's node can view the entry referenced by the story. Deletion uses the visibility filter captured in the
deletion liberin. Owner name and original node name are assumed to coincide. Timeline republishing refers to the
posting as a whole and omits the revision ID. A remote story is identified by its ID; feed name and moment are
descriptive metadata and do not participate in deduplication or unsharing matches. Notifications carry the current
story moment.

The receiver validates the sharing type, posting ID, optional revision, and complete remote reference. Sharing
mutations lock the tenant's original posting, deduplicate or remove exact matching sharings, and update the counter.
For every new sharing, the processor persists a `PostingSharedJob` in the same transaction through
`Jobs.runAfterCommit`, with the local node ID, sharing identity, and optional avatar snapshot. The job resolves any
matching avatar from the contact before downloading it, then locks the posting and completes the sharing only if it
still exists. The job cannot recreate a sharing removed by a subsequent unshared notification. It initiates the added
liberin after completion, including when the notification has no avatar. Before deletion, sharing mutations capture
the IDs of related instant substories for cleanup after commit. Committed liberins initiate instant creation or cleanup
and send sharing-added or sharing-deleted events, independently of total events. List and total events use their own
viewing permission and posting visibility. The creation receptor locks the posting and verifies that the sharing
still exists, so a delayed added liberin cannot recreate an instant or announce a deleted sharing. Event payloads are
defined in `moeraorg.github.io/_data/events.yml`.

Posting subscribers receive counter updates through `POSTING_SHARINGS_UPDATED` when their access permits it. The
picked copy updates its counter and emits the total event; it does not own the original sharing list. Public list and
permission fields are specified in the [Node API](https://moera.org/development/api/node/), and notification payloads
are defined in `moeraorg.github.io/_data/notifications.yml`.

## Node source URI

Every public structure that carries a node full-name snapshot also carries an optional source URI snapshot. The usual
field-name rule replaces the final `FullName` with `SourceUri`, preserving any prefix. Profile structures use the
shorter `sourceUri` name because the subject is already unambiguous.

The source URI is historical identity metadata. Creation and ingestion code captures it from the same profile or
notification object as the full name and stores both in one transaction. Conversion helpers return the stored pair;
they do not decorate an old object with the node's current profile. A contact refresh may update its current snapshot,
but it does not rewrite older entries, reactions, stories, or moderation records.

The value is optional, at most 1024 characters, and is not validated as a URI by `moera-node`. Omission leaves a
profile value unchanged on update; an empty profile attribute resets it.

Moeralib provides `NodeSourceUri` helpers for the serialized form:

```text
<uri>;<parameter-name>=<parameter-value>;...
```

The first semicolon begins the parameter block. Names and values use UTF-8 RFC 3986 percent encoding, duplicate names
keep their first position and last value, empty names are ignored, and unknown parameters are preserved. `via_email`
is the currently published parameter name. The node itself stores and transports the full string as opaque metadata.

## Federated OCR metadata

Locally recognized media text is stored as `MediaFile.recognizedText` and exposed as `textContent`. For every local
owner of a non-empty recognized result, a liberin sends `LEASED_MEDIA_TEXT_UPDATED` to nodes holding a lease for that
owner.

The receiving processor first verifies the exact tuple of local node, sender node, media ID, and lease ID. It then
schedules a persistent after-commit `MediaTextUpdatedJob`. The job updates only matching `RemoteMediaFile` rows,
recomputes derived headings and descriptions, invalidates attachment caches, and emits media-text and heading changes.
If the lease has disappeared before the job runs, the operation becomes a no-op.

Media-text notifications carry an optional media node name so equal media IDs from different nodes cannot collide in
posting or search updates. Empty OCR results mark recognition complete on the source node but do not clear text already
propagated to lease holders.

## Search subscribers

Moera Search participates as a normal subscriber to search-oriented notifications. Node receptors send full posting,
comment, reaction, profile, and incremental media-text updates through the same durable notification pipeline used for
other peers.

Removing a posting story from a public feed sends a search update to subscribers that can see the posting. The update
identifies the removed story and the posting, preserving the original posting identity for picked copies. When posting
deletion also removes its stories, those story deletions suppress their search updates; the posting receptor sends the
posting deletion update instead. The deletion context is captured before the transaction closes, so delivery does not
depend on loading a deleted story or its relationships. See
[`StoryDeletedLiberin`](../../src/main/java/org/moera/node/liberin/model/StoryDeletedLiberin.java),
[`StoryOperations`](../../src/main/java/org/moera/node/operations/StoryOperations.java), and
[`StoryReceptor`](../../src/main/java/org/moera/node/liberin/receptor/StoryReceptor.java).

Source URI fields and remote-media node names are retained in these payloads. This lets search preserve historical
identity metadata and update the correct attachment when two nodes use the same media ID. The search index itself is
owned by the separate `moera-search` service; it is not part of the node's PostgreSQL schema.
