# Asynchronous processing

[Architecture index](../architecture.md)

## Signals, work, and delivery

These mechanisms have different ownership and recovery contracts:

| Mechanism | State and recovery owner |
| --- | --- |
| Liberin | Transient domain-change signal; `LiberinManager` invokes a receptor in a separate write transaction |
| Job | Executable intent/checkpoint in `pending_jobs`; `Jobs` restores and retries it |
| Pick | Remote download/publication intent in `picks`; `PickerPool` owns bounded retries |
| Peer notification | Normally backed by `pending_notifications`; sender owns remote delivery |
| WebSocket event | Protected local update with transient replay state in `EventManager` |
| SSE push | Offline packet rows plus bounded live queues, managed by `PushService` |
| FCM relay / mail | Separate in-memory delivery queues; no restart recovery |

A task only supplies execution/context handling; it is not durable by itself. For worker coordination and multi-instance
limitations, see [runtime](runtime.md#process-and-deployment-boundary). Protocol-specific recovery belongs to
[federation](federation.md); physical deletion uses [storage tombstones](media-storage.md#cleanup).

## Commit boundaries

HTTP code collects liberins in `RequestContext`. `AfterCommitLiberinsInterceptor` sends them when MVC completion has no
exception. This is request-completion logic, not a database outbox. Background `Transaction` scopes use
`UniversalContext` to hold liberins until commit and discard them on rollback. Nested holding scopes merge, although
their database transactions commit independently. Do not wrap this in a second holding mechanism.

[LiberinManager](../../src/main/java/org/moera/node/liberin/LiberinManager.java) delivers signals from an in-memory
queue. Receptors update derived state or initiate delivery/jobs; a consumed signal that fails is logged without durable
replay. A crash between the primary commit and delivery can lose its side effects. Pass IDs or captured deletion
context across this boundary and recheck current existence before acting on delayed work.

Scheduling APIs differ in when intent becomes durable:

| API | Persistence and start |
| --- | --- |
| `Jobs.runAfterCommit()` | Pending row participates in caller transaction; execution starts after commit |
| `Jobs.run()` | Independent persistence attempt, immediate execution; storage errors can leave only transient work |
| `Jobs.runNoPersist()` | Immediate best-effort work |
| `PickerPool.pickAfterCommit()` | Callback persists/starts the pick after commit, leaving a commit-to-callback gap |

Without an active transaction, after-commit APIs start immediately after persistence. Use `Jobs.runAfterCommit()` with
the local node ID when mutation and job intent must commit together. This is required for long work delegated from
incoming notification handlers. A pick callback prevents work on rollback but does not commit a pick atomically with
the mutation that requested it.

Implementation: [Transaction](../../src/main/java/org/moera/node/util/Transaction.java),
[Jobs](../../src/main/java/org/moera/node/task/Jobs.java), and
[JobsTest](../../src/test/java/org/moera/node/task/JobsTest.java).

## Jobs and retry ownership

Explore checks warm domains periodically against each domain's stored next update time. For a due domain, it submits
the fetch job and then saves the randomized next update time.
See [RecommendationOperations](../../src/main/java/org/moera/node/operations/RecommendationOperations.java).

Each job chooses its transaction scopes, checkpoint state, retry classification, and any class parallelism limit.
`Jobs` restores committed pending rows and submits due attempts; success or permanent failure removes the row, while
retry persists state and next execution time. Unexpected exceptions normally fail an attempt permanently unless the
concrete job overrides that behavior. Executor rejection postpones execution.

Persisted job class names and JSON parameters/state are a compatibility boundary: renaming a class or changing payload
shape must account for already queued work. External effects can repeat after a crash before checkpoint/removal;
receivers and final writes need deduplication, existence checks, or fencing. There is no system-wide exactly-once
transaction. Outgoing notification and SSE persistence also use quiet error handling, so live delivery alone does not
prove that a recovery record was stored.

## Client delivery

[EventManager](../../src/main/java/org/moera/node/event/EventManager.java) authenticates STOMP sessions and maintains a
short-lived replay queue. A new client saves the cursor offered at connection and reuses it after an interruption.
When expiry or a restart makes replay incomplete, the node asks that subscriber to refresh its cached data and then
sends the retained events in order. It checks each queued event against the recipient's tenant and permissions,
then protects fields on a copy so the original remains available for other recipients and later replay.

SSE push uses registered clients and a moment cursor. `PushClients` stores offline packets; `Pusher` combines those
slices with a bounded live queue, falling back to stored data under pressure. Retention is bounded. SSE requires admin
`OTHER` scope, and story push content omits external source references.

FCM relay delivery signs remote calls, has bounded retries, and can disable activity when no devices remain. Mail queues
preparators for rate-limited SMTP delivery and logs failed sends without automatically retrying the failed message.
Both queues and their retry state are transient. Adapter implementation lives in
[presentation and integrations](presentation-and-integrations.md#adapters).

[EventManagerTest](../../src/test/java/org/moera/node/event/EventManagerTest.java) covers recipient/replay isolation.
