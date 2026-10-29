# Picker media identity fix

The approved scope is a forward-only change to Picker's choice of local media owners. Existing attachment mappings
and child postings remain unchanged; no schema or data migration is required.

Owner reuse is scoped to the local parent entry and the remote node name and media ID. Search all retained revisions
and verify that the owner still refers to the prepared content. A previously unseen identity receives a new owner
while retaining content deduplication through `MediaFile`. Other download callers keep their existing behavior.

- [x] Add PostgreSQL regression tests for distinct remote IDs with identical content across revisions, repeated
   identities in one revision, and reintroducing an attachment from an older revision. Observe failure before changing
   production code. Use real attachment lookup in the fixture and update its obsolete Picker dependency.
- [x] Add an explicit repository query, a media ownership method accepting remote identity, and pass the actual parent
   entry ID and remote identity from Picker. Keep the generic ownership method available to other callers.
- [x] Document the ownership invariant in `doc/architecture/media.md`. Run the regression tests, relevant unit tests,
   project checks, and a fresh code review. Stop the isolated test database after validation.

The fix does not reconcile historical mappings that already share an owner across different remote identities.

Validation: the regression reproduced the original unique-constraint failure before the production change.
After the change, all 12 PostgreSQL integration tests and all 435 tests in the normal Maven suite passed.
Checkstyle reported no violations. Independent code review found no actionable issues.
