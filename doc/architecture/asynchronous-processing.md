# Asynchronous processing

## Task execution

`Task` is the base for work that runs outside an HTTP request. `TaskAutowire` creates or injects a task and gives it a
`UniversalContext`, a transaction helper, the remote Node API, and either a particular hosted node or no domain for
global work. The task wrapper centralizes context setup, logging, and exception handling.

The application uses separate bounded executors for remote jobs, federation notification senders, pickers, naming,
push work, and other scheduled tasks. Rejection from an executor is treated as backpressure: components either queue,
pause, or reschedule their work.

## Persistent jobs

`Job<P, S>` extends `Task` with serializable parameters, optional checkpoint state, retry policy, and a persistent ID.
`Jobs` stores durable instances in `pending_jobs` and keeps near-term instances in an in-memory priority queue. On
startup and once per hour, it loads persisted jobs whose next execution falls within the next hour.

A job may be associated with one hosted node or be global. Retry policies include no retry, a fixed retry count,
exponential backoff, and a fixed-then-exponential sequence. Jobs can also cap the number of parallel instances of their
class.

Calling `Job.success()`, `Job.fail()`, or `Job.retry()` terminates the current attempt by throwing an internal control
exception. A completed or permanently failed job is removed from `pending_jobs`; a retry stores its state, retry count,
and next execution time.

`Jobs.runAfterCommit()` persists the job inside the caller's transaction and registers its in-memory execution only
after commit. This is used when local state must refer to the job ID atomically, such as video compression or handling
an incoming OCR update. A rollback leaves neither a runnable task nor a committed pending job.

Non-persistent jobs are available for best-effort work that does not need restart recovery. They still use the same
task context and executor.

## Liberins

A liberin is an internal description of a completed domain change. Controllers and operations send liberins instead of
calling all secondary consumers directly. `LiberinManager` routes each concrete liberin type to methods on
`@LiberinReceptor` components.

Liberins are transaction-aware:

- request code adds them to `RequestContext`, and the MVC after-commit interceptor dispatches them after success;
- background transactions are held by `UniversalContext` and released by `Transaction` after commit;
- a rollback discards held liberins; and
- nested background transactions merge their liberins into the outer holding scope.

Receptors are the integration point for derived side effects. Depending on the change, they update instant stories,
publish local events, queue signed notifications to other nodes, send push or mail messages, invalidate caches, or
start further jobs. This preserves the transaction boundary of the primary operation and gives related reactions one
consistent ordering point.

`StoryUpdatedLiberin` carries only the story ID. `StoryReceptor.updated()` reloads the story by node and story ID in the
receptor's transaction before building client events and push content. Lazy relationships therefore belong to the
active session, and updates for stories deleted before delivery are skipped.

`PostingOperations` clears a posting's recommendation flag and emits
`PostingRecommendationUpdatedLiberin` with the posting ID.
`PostingReceptor` reloads the posting and sends a visibility-filtered `posting-updated` client event. This local flag
change does not initiate content-edit notifications to other nodes.

## Client events

`EventManager` distributes protected events to authenticated WebSocket/STOMP sessions. It maintains a time-limited
queue in memory, assigns monotonically increasing ordinals within the process, and lets reconnecting clients report the
queue start and last seen ordinal. A changed queue start tells a client that its previous cursor is no longer usable.

Before delivery, the queued event checks its principal filter. `EventManager` then copies the packet and event,
including nested payload objects, and removes fields the subscriber is not allowed to see from the copy. Permission
state and packet metadata are preserved. Protection never modifies the queued payload or packets sent to other
subscribers, so later deliveries and replays use the full original data. A daemon delivery thread sends queued events,
while a scheduled task retries delivery and emits ping events once per minute. The queue is transient across process
restarts.

Only subscribers with administrative `VIEW_CONTENT` scope receive external source metadata in story events.

`PushController` also exposes an SSE stream backed by the push subsystem. Its bounded in-memory queue falls back to
stored offline packets when a consumer cannot keep up. Mobile push delivery is handled separately through the FCM
relay integration.

SSE push subscriptions use administrative `OTHER` scope. Their story packets omit external source references.

## Scheduled maintenance

Spring scheduling runs database and storage maintenance such as:

- loading and restarting pending jobs;
- retrying picks and federation senders;
- recalculating contact distance and visited-node counts;
- expiring uploads, media leases, unused media, drafts, and frozen notifications;
- uploading eligible media to S3 and removing media tombstones;
- OCR scheduling, feed maintenance, reminders, and sitemap work; and
- retrying client events and push delivery.

Scheduled methods use `RequestCounter.allot()` to register an active operation for diagnostics. `RequestCounter`
assigns it an MDC request ID, logs the number of concurrent operations, and warns when an operation runs for more than
a minute. Global maintenance iterates warm domains and associates `UniversalContext` before reading or changing
tenant-specific state.

## Coordination patterns

The asynchronous subsystems use several distinct mechanisms:

| Need | Mechanism |
| --- | --- |
| survive a restart | database row in `pending_jobs`, `pending_notifications`, `picks`, or a focused queue table |
| start only after commit | transaction synchronization or held liberins |
| serialize by remote node | one sender or picker per local-node/remote-node direction |
| serialize by media identity | PostgreSQL advisory lock using the media hash |
| distribute work across instances | short row claim with `FOR UPDATE SKIP LOCKED` and a renewable lease |
| avoid overlap inside one process | `AtomicBoolean` or a keyed in-memory lock |
| tolerate retry | idempotent update, fenced lease token, or revalidation under a lock |

External calls are kept outside database transactions when they may block. The worker first captures or claims a
snapshot, performs the external operation, then publishes only if the database still matches the snapshot.
