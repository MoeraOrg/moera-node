# Transactions during remote media downloads

`Picker` and `RemoteMediaDownloadJob` prepare remote media before opening their final write transaction.
`MediaManager.preparePrivateMedia()` must be called without an enclosing transaction. It checks existing owners
and the remote cache in short read transactions, downloads and verifies missing content, and returns scalar data.
The existing `MediaOperations.putInPlace()` and remote cache operations commit stored content independently.

`MediaManager.ownPreparedPrivateMedia()` runs inside the caller's write transaction. It resolves the current owner
again, or creates an owner from the verified local file. It never downloads from a peer node. If the media hash lock
is busy, it throws a retryable exception instead of waiting for another download while holding a transaction.
An unavailable prepared file also causes a retry instead of a download inside the write transaction.

`Picker` reads the posting ID, revision ID, modification time, and remote attachment keys before preparing media.
Its final transaction locks and reloads the posting, checks that the snapshot still matches, and saves the posting,
revision, attachment owners, attachments, permissions, reaction totals, and sources together. Missing supplied
parents cause a retry. Existing attachments are read again in the final transaction, including owners added by
other background jobs during preparation. Liberins and child picks are dispatched after a successful commit.

`RemoteMediaDownloadJob` creates/reuses the owner and builds `PrivateMediaFileInfo` in the same write transaction,
so lazy preview collections are available while building the response.

Verified content may remain cached after a later transaction rolls back. No partially updated posting or newly
created attachment owner is committed by that failed transaction. Remote lease requests still have their existing
failure semantics: they cannot be rolled back with local database changes.

Other callers of the older download methods retain their transaction behavior. Local preview processing, including
possible retrieval of cloud-only content, still happens in `MediaOperations.own()` inside the write transaction.

## Verification

The regular unit tests check network calls without an active transaction/connection and a busy hash lock returning
immediately. `MediaDownloadTransactionsIT` additionally uses real Hibernate repositories, PostgreSQL transactions,
and the project migrations. Network responses and media processing are controlled test substitutes.

Create an isolated, empty PostgreSQL database accessible as the current OS user with an empty password, then run:

```sh
byte_buddy_agent=/home/balu/.m2/repository/net/bytebuddy/byte-buddy-agent/1.18.11/byte-buddy-agent-1.18.11.jar
mvn -o test \
  -Dtest=MediaDownloadTransactionsIT,MediaManagerTest,PickerTest,RemoteMediaDownloadJobTest \
  -Dmoera.test.database=jdbc:postgresql://127.0.0.1:55439/media_transactions \
  -DargLine=-javaagent:$byte_buddy_agent \
  -Dskip.npm -Dskip.installnodenpm -Dskip.install-node-and-npm
```

The integration test installs `uuid-ossp`, applies migrations, and inserts test data. Use a disposable database.
It covers new postings, updates with attachment reuse, rollback, stale snapshots, missing parents, leases,
owner rebinding, lazy previews, wrong hashes, and streaming before the posting write transaction.
