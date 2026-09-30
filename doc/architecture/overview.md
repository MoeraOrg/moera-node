# Overview

## Responsibilities

A `moera-node` process is the server-side home of one or more Moera identities. Its main responsibilities are:

- expose the Node API under `/moera/api`;
- render a limited public web interface for simple clients, search engines, and AI agents;
- persist profiles, postings, comments, revisions, reactions, social relationships, feeds, and moderation data;
- communicate with other nodes through the Node API and signed asynchronous notifications;
- store, transform, authorize, and serve media;
- deliver local client events and push notifications; and
- run scheduled and retryable background operations.

The full browser client is `moera-client-react`. The Web UI in this repository is a separate, intentionally limited
interface.

## Runtime stack

The application is a Java 21 Spring Boot 4 application built with Maven. Spring MVC provides REST and HTML endpoints,
Spring Data JPA and Hibernate access PostgreSQL, and Flyway applies schema migrations. Scheduling and transaction
management are enabled at the application level.

Handlebars renders HTML and mail templates. Jackson serializes API, notification, event, job, and cache payloads.
QueryDSL supports constructed database predicates. The build also runs a small Node-based frontend pipeline for static
assets.

Media processing adds filesystem and optional AWS S3 storage, Apache Tika and metadata-extractor for type and metadata
detection, TwelveMonkeys and Thumbnailator for images, and external `ffprobe` and `ffmpeg` processes for video.

## Main component flow

An ordinary API request follows this shape:

1. MVC interceptors select the hosted node, build request identity and access state, and establish request metadata.
2. A controller validates generated API structures and coordinates domain operations.
3. Operation components change JPA entities and enqueue internal liberins in the same transaction.
4. After commit, liberin receptors update derived state and create stories, local client events, remote notifications,
   mail, push messages, or additional jobs.
5. Conversion helpers under `model` build generated API, event, and notification structures from stored state.

Remote operations often replace the synchronous third step with a persistent `Job`. Incoming federation notifications
are authenticated synchronously and may schedule a durable after-commit job for the actual work.

## Source layout

| Area | Main packages | Responsibility |
| --- | --- | --- |
| HTTP API | `rest`, `global` | REST controllers, request annotations, validation failures, exception mapping |
| Web UI | `ui`, `ui.helper`, `ui.sitemap` | Handlebars pages, media pages, RSS, sitemaps, crawler-facing output |
| Persistence | `data` | JPA entities and repositories |
| Domain logic | `operations`, `subscriptions`, `friends`, `userlist` | Stateful social operations and caches |
| Context | `domain`, `global`, `option`, `config` | Hosted domains, per-node options, request/background identity |
| Federation | `api`, `notification`, `picker`, `rest.task` | Naming, remote calls, notifications, remote content |
| Internal reactions | `liberin`, `instant`, `event` | Post-commit fan-out, feed stories, client events |
| Durable work | `task` | Task wiring, persistent jobs, retry policies |
| Media | `media`, `ocrspace` | Storage, grants, upload, serving, transforms, OCR, cleanup |
| Text | `text` | Markdown conversion, sanitization, headings, descriptions, shortening |
| Integrations | `mail`, `push`, `indexnow`, `linkpreviewnet` | External delivery and metadata services |

## Architectural boundaries

Controllers own HTTP concerns. They should delegate reusable state transitions to operation or focused service
components. JPA entities hold persisted state and relationships; API conversion and workflow logic do not belong in
entity accessors.

Generated moeralib types are the wire contract. Handwritten `model` helpers translate between those types and the
persistence model. This keeps protocol evolution separate from database representation.

Federation is asynchronous where remote availability would otherwise extend a local write transaction. Persistent
jobs, pending notifications, and picks retain work across process restarts. Local client events are transient and are
kept only in the running process.

Database transactions protect local state. Network calls and large file transfers are normally performed outside long
write transactions, followed by a short transaction that validates the snapshot and publishes the result.
