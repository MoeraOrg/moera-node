package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BlockedInstantRepository extends JpaRepository<BlockedInstant, UUID> {

    @Query("select bi from BlockedInstant bi where bi.nodeId = ?1 and bi.id = ?2")
    Optional<BlockedInstant> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select bi from BlockedInstant bi where bi.nodeId = ?1 and bi.storyType = ?2 and bi.entry.id = ?3")
    Optional<BlockedInstant> findByStoryTypeAndEntryId(UUID nodeId, StoryType storyType, UUID entryId);

}
