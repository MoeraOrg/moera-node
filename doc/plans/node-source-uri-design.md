# Node source URI design

## Goal

Every public Node API value that identifies a node by a full name must also carry the optional URI from which that
identity originates. The URI is historical metadata: when a full name is copied into a posting, reaction, notification,
instant, complaint, or other stored object, the URI visible at that time is copied and retained with it.

Client application code is outside this change. The supported repositories are `moera-node`, the generated Moera
libraries for every language, and `moera-search`.

## Public API contract

The new fields are optional strings. They have a maximum length of 1024 characters and no other validation, including
no URI syntax validation.

The naming rule is mechanical:

| Existing field | New field |
| --- | --- |
| `fullName` | `nodeSourceUri` |
| `ownerFullName` | `ownerSourceUri` |
| `receiverFullName` | `receiverSourceUri` |
| `remoteFullName` | `remoteSourceUri` |
| `summaryFullName` | `summarySourceUri` |
| `nodeFullName` | `nodeSourceUri` |
| `postingOwnerFullName` | `postingOwnerSourceUri` |
| `commentOwnerFullName` | `commentOwnerSourceUri` |
| `remoteNodeFullName` | `remoteNodeSourceUri` |
| `remotePostingOwnerFullName` | `remotePostingOwnerSourceUri` |
| `remoteCommentOwnerFullName` | `remoteCommentOwnerSourceUri` |

Longer or newly discovered prefixes follow the same replacement rule: replace the terminal `FullName` with
`SourceUri`, retaining the prefix. A source URI is added to every API structure containing one of these full-name
fields, including inherited fields used by notifications.

There are three explicit profile exceptions. `ProfileAttributes`, `ProfileInfo`, and `WhoAmI` use `sourceUri`, because
the node whose profile is being edited or returned is already unambiguous in those structures. `ProfileAttributes`
accepts an omitted value as “leave unchanged” and an empty string as “reset”, matching `fullName` and the other profile
attributes. `ProfileInfo` and `WhoAmI` return `null` when it is unset.

The source schema is `../moeraorg.github.io/_data/node_api.yml`. Generated Java, Python, and TypeScript Moera libraries
are rebuilt from it. Generated files under client application repositories may be changed temporarily by the generator
but are excluded from the final diff.

## Local profile data

The node's own source URI is stored in the options database as `profile.source-uri`. It is added to `options.yaml`,
written by `ProfileAttributesUtil`, and read by both variants of `ProfileInfoUtil` and by `WhoAmIiUtil`.

`RequestContext.sourceUri()` and `UniversalContext.sourceUri()` expose the value alongside `fullName()`. Task and
notification base classes that currently expose the local full name also expose the local source URI. This gives HTTP
requests and background jobs the same value without associating unrelated thread context.

Profile and node-name update events carry the source URI. A change of only the source URI must still propagate to nodes
that cache profile details.

## Persistence

Migration `V307` adds nullable `varchar(1024)` columns beside every persisted full-name copy. The minimum required
mapping is:

| Table/entity | Existing value | New value |
| --- | --- | --- |
| `entries` | owner, receiver, replied-to full names | matching owner, receiver, replied-to source URIs |
| `entry_sources` | remote full name | remote source URI |
| `drafts` | owner full name | owner source URI |
| `reactions` | owner full name | owner source URI |
| `own_postings`, `own_comments`, `own_reactions` | remote and replied-to full names present in each entity | matching remote and replied-to source URIs |
| `contacts` | remote full name | remote source URI |
| `stories` | remote, remote posting, and remote owner full names | matching source URIs |
| sheriff order, complaint, and complaint-group tables | every stored node/owner full name | matching source URI |

The final implementation inventory is driven by all JPA fields ending in `FullName`, rather than only this table. Each
persisted full-name field must have a matching nullable source-URI field unless the full name is derived rather than
stored. Existing rows are not backfilled and therefore keep `NULL` source URIs.

Entity setters and getters only copy values. Conversion helpers are responsible for moving the URI between generated
API types and entities. Repository queries remain explicit, and projections or update queries that enumerate columns
are extended together with their entity mappings. `doc/create_tables.sql` remains generated and is not edited.

## Data flow

At every capture point, the full name and source URI come from the same profile snapshot:

1. Local creation paths read both values from `RequestContext` or `UniversalContext`.
2. Remote API calls read both from the same `WhoAmI`, `ProfileInfo`, posting, comment, reaction, or notification object.
3. Persistence operations save both values in the same transaction.
4. API conversion helpers return the stored pair. They do not fetch a current URI to decorate historical data.

This applies to postings, comments, replied-to metadata, posting sources, drafts, reactions, contacts, recommendations,
instant stories and their summaries, sheriff orders and complaints, notifications, liberins, and externally visible
events. Constructors and helper signatures that currently accept a full name gain the corresponding source URI next
to it. JSON-backed liberins and events include the field so delayed processing does not lose it.

Remote profile refresh updates the contact's full name, source URI, and related profile data together. Downstream events
must be emitted when either the full name or source URI changes. Cached posting/reaction history is not rewritten by a
later profile update.

## Notifications and fingerprints

`NotificationPacket` gains `nodeSourceUri`. The sender fills it from the local profile; receivers copy it into sender
metadata and any derived events or instants.

Because notification packet version 1 signs `fullName`, a new fingerprint version signs `nodeSourceUri` as well. The
sender emits the new latest version. Verification retains versions 0 and 1 so packets from older nodes remain valid.
`moera-node`, `java-moeralib`, and `moera-search` use the same field order for the new fingerprint version.

For other signed structures, a source URI is added to a fingerprint only where its paired full-name field already
participates in that fingerprint. Existing fingerprint versions remain available for old objects.

## Moera Search

`moera-search` stores source URIs as Neo4j properties next to the existing full-name properties for nodes, postings,
comments, reactions, and any other indexed object that retains a full name. Ingestion takes both fields from the same
Node API object. Result builders return the stored URI and use the node property's URI as the same fallback currently
used for a missing entry owner full name.

The search service's own `WhoAmI.sourceUri` is configurable alongside `nodeFullName`. Notifications received by the
search service retain `NotificationPacket.nodeSourceUri` as sender metadata. Old graph records without the property
return `null`; no bulk backfill is required.

## Compatibility and generated repositories

All fields are optional, so older stored data and payloads remain readable. New notification signatures use a versioned
fingerprint, preserving verification of older packets. No moeralib snapshot version is changed as part of generation.

The API schema and generated library repositories already contain unrelated uncommitted user changes. Regeneration must
preserve those changes. Before and after generation, their diffs are compared so the final result contains both the
pre-existing edits and the source-URI additions.

## Verification

Behavior tests are written before manual production changes and must demonstrate:

- profile `sourceUri` update, reset, `ProfileInfo` output, and `WhoAmI` output;
- local and remote posting/comment/reaction capture and round-trip of the matching source URI;
- contact refresh when only source URI changes;
- instant/story persistence and summary output;
- notification packet population, new fingerprint verification, and compatibility with older fingerprint versions;
- sheriff and other conversion helpers that persist composite owner fields;
- `moera-search` ingestion and output, including missing properties on old graph records.

After generation, build every generated moeralib supported by the repository tooling, install the Java artifact used by
the services, compile and test `moera-node` and `moera-search`, and run `git diff --check` in every touched repository.
