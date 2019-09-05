package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EntryRevisionRepository extends JpaRepository<EntryRevision, UUID> {
}
