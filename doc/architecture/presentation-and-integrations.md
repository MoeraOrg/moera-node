# Presentation and integrations

## Built-in web interface

The Web UI is the server-rendered interface built into `moera-node`. It serves public profiles, timelines, postings,
people lists, RSS, sitemaps, error pages, and browser-friendly media. It is intended for limited clients, search
engines, and AI agents. It is separate from the full `moera-client-react` application.

Spring MVC controllers under `ui` prepare view models and Handlebars renders templates from
`src/main/resources/templates`. Components annotated with `@HelperSource` register Handlebars helpers at startup.
Static fonts, JavaScript, images, and compiled styles are packaged under `src/main/resources/static`.

Per-node options decide whether Web UI pages are enabled and whether interactive users are redirected to the full
client. User-agent classification lets crawler-facing HTML remain accessible under that policy.

## Discovery and indexing

The UI layer produces `robots.txt`, RSS, and XML sitemaps. Sitemap records are persisted so large dynamic sets can be
split into stable sitemap documents. Entry changes update the relevant records through the normal liberin and
maintenance flow.

`IndexNowClient` submits changed public URLs to the configured IndexNow endpoint. Search-engine request interceptors
and statistics components identify crawler traffic and record search clicks separately from ordinary client activity.

Moera Search receives structured subscription notifications through the federation pipeline. Search ingestion is an
external consumer and does not share the node's database.

## Local realtime clients

`EventManager` exposes protected local events over WebSocket/STOMP. Events cover entry, story, reaction, subscription,
profile, moderation, media, and other node changes. Each event protects its fields and checks permissions for the
subscribed session before delivery.

The push subsystem persists offline `PushNotification` rows and exposes an SSE endpoint under
`/moera/api/push/{clientId}`. It also builds mobile push payloads and sends them through the configured FCM relay. The
relay is accessed through the generated push-relay client in `api.pushrelay`.

## Mail

Spring Mail provides SMTP delivery. Typed mail components render localized Handlebars templates for password reset,
email verification, provider-domain creation, node deletion, and moderation complaints. The mail service converts HTML
templates to a text alternative and reports send or template failures through focused exceptions.

SMTP connection settings are Spring deployment properties. Node-specific mail behavior and public URLs come from the
active hosted-node options and context.

## Link previews

`LinkPreviewNet` calls the configured link-preview service. The resulting metadata passes through node validation and
media handling before it is returned or embedded. Public and private media URLs are resolved through the same direct
serving abstraction as other server-rendered media, so filesystem-relative and absolute S3 URLs work consistently.

## OCR.Space

`OcrSpace` is the current OCR service adapter. `OcrJob` invokes it asynchronously with either an absolute direct media
URL or multipart file content. The adapter has separate errors for connectivity, malformed responses, and recognition
failure so the job can distinguish retryable service problems from completed processing.

OCR results enter the media and entry model through `OcrOperations`; propagation to remote lease holders uses signed
Moera notifications rather than a direct OCR-specific transport.

## AWS S3

`AwsS3MediaStorage` owns the lazy AWS SDK clients, credential provider, multipart upload, download, exact-key deletion,
URI parsing, and presigning. `AwsS3MediaUploader` owns database claims and publication. Keeping these responsibilities
separate lets unit tests substitute storage calls and keeps database transactions out of long network transfers.

The S3 bucket and lifecycle policy are provisioned externally. The node stores only object keys and upload leases in
PostgreSQL; credentials remain in the AWS provider chain or selected shared profile.

## Naming service

The naming service maps a registered Moera name and generation to the node URI and signing key. Naming lookup is used
for outbound Node API clients and inbound signature verification. `NamingCache` limits repeated remote lookups, while
background senders classify naming failures separately because a name may become resolvable later.

## Configuration ownership

| Integration | Deployment configuration | Per-node state |
| --- | --- | --- |
| SMTP | Spring mail properties | profile URLs, locale, and mail-related options |
| FCM relay | `node.fcm-relay` | registered push clients and queued messages |
| IndexNow | `node.index-now` | public entry and sitemap state |
| link preview | `node.link-preview` | permissions and resulting media references |
| OCR.Space | `node.media.ocr-service*` | OCR schedule and recognized media text |
| AWS S3 | `node.media.direct-serve.*` | shared media rows and attachment caches |
| naming | per-node naming location plus client configuration | registered node name and signing material |

External calls are made from focused clients or background workers. Domain operations retain durable intent before a
call when loss would matter, and apply returned data through the same transaction, liberin, and permission boundaries
used by local changes.
