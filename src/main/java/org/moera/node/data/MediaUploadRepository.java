package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaUploadRepository extends JpaRepository<MediaUpload, UUID> {

    @Query("select mu from MediaUpload mu where mu.nodeId = ?1 and mu.id = ?2")
    Optional<MediaUpload> findByNodeIdAndId(UUID nodeId, UUID id);

    @Query("select mu from MediaUpload mu where mu.deadline < ?1")
    List<MediaUpload> findExpired(Timestamp deadline);

    @Query("delete from MediaUpload mu where mu.nodeId = ?1 and mu.id = ?2")
    @Modifying
    void deleteByNodeIdAndId(UUID nodeId, UUID id);

}
