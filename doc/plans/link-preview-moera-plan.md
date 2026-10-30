# Moera posting link previews

> Execution: implement in this session using `superpowers:executing-plans` and focused regression tests.

**Goal:** Include posting owner metadata in signed link previews created for Universal Moera posting URLs.

**Architecture:** Extend the public `LinkPreview` schema and handwritten Java body model. In the client, use the
existing universal URL parser, posting cache, and node API from the shared preview loader. Carry owner metadata through
preview state into the editor's body, excluding volatile avatar paths.

**Tech stack:** YAML API schema, generated Java/Python/TypeScript libraries, React, Jest, Maven.

**Spec:** User request in this conversation: five optional owner fields; only Moera posting URLs; recognized domains
`moera.page` and `web.moera.org` in an extensible constant; cached posting first, otherwise fetch; no title or image
hash; description from `PostingInfo.heading`; document why signed previews must omit avatar `directPath`.

## Constraints and review focus

- Preserve existing changes in all sibling repositories and keep library snapshot versions.
- Use existing universal URL parsing and node-name expansion, including encoded names and explicit node locations.
- Other hosts and non-posting URLs retain normal preview loading.
- Omit avatar `directPath` and `directPathExpiresAt` without mutating the cached posting.
- A failed posting fetch releases preview loading; optional owner data and empty headings remain valid.
- Check final editor values, not only the intermediate preview state.

## Tasks

- [x] Add Java regression coverage for owner metadata surviving body re-encoding.
- [x] Extend `../moeraorg.github.io/_data/node_api.yml`, run `../update-api` once, update handwritten Java
  `LinkPreview`.
- [x] Add failing client tests for cached/fetched postings, other URLs, errors, and editor body metadata.
- [x] Add a universal posting URL helper, extend preview state, and populate the editor's preview from `PostingInfo`.
- [x] Document the signed-body invariant in `doc/architecture/presentation-and-integrations.md`.
- [x] Verify with focused Jest tests, TypeScript compilation, `mvn clean install` in `../java-moeralib`, node
  compilation, and `git diff --check` in all touched repositories.
- [x] Review the complete change and resolve substantive findings: independent review found none.

## Verification results

- Both focused client suites pass: 18 tests.
- Java library rebuild and install pass: 77 tests, including owner metadata surviving body re-encoding.
- Node compilation and `git diff --check` in all six changed repositories pass.
- TypeScript checks pass for all client source files and the new tests. Checking every existing test also reports
  unrelated implicit-`any` errors in existing emoji, instant, and sharing tests.
- Previously modified files retain their changes; regeneration adds only the LinkPreview schema and its artifacts.
