package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface OwnReactionRepository extends JpaRepository<OwnReaction, UUID> {

    @Query("select r from OwnReaction r where r.nodeId = ?1 and r.remotePostingId in (?2)")
    List<OwnReaction> findAllByRemotePostingIds(UUID nodeId, List<String> remotePostingIds);

    @Query("select r from OwnReaction r where r.nodeId = ?1 and r.remoteNodeName = ?2 and r.remotePostingId = ?3")
    Optional<OwnReaction> findByRemotePostingId(UUID nodeId, String remoteNodeName, String remotePostingId);

    @Modifying
    @Query("delete OwnReaction r where r.nodeId = ?1 and r.remoteNodeName = ?2 and r.remotePostingId = ?3")
    void deleteByRemotePostingId(UUID nodeId, String remoteNodeName, String remotePostingId);

}
