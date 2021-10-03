package org.moera.node.data;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaFileOwnerRepository extends JpaRepository<MediaFileOwner, UUID> {

    @Query("select mo from MediaFileOwner mo left join fetch mo.mediaFile where mo.nodeId = ?1 and mo.id = ?2")
    Optional<MediaFileOwner> findFullById(UUID nodeId, UUID id);

    @Query("select mo from MediaFileOwner mo left join fetch mo.mediaFile where mo.nodeId = ?1 and mo.id in (?2)")
    Set<MediaFileOwner> findByIds(UUID nodeId, UUID[] ids);

    @Query("select mo from MediaFileOwner mo where mo.nodeId = ?1 and mo.ownerName is null and mo.mediaFile.id = ?2")
    Optional<MediaFileOwner> findByAdminFile(UUID nodeId, String mediaFileId);

    @Query("select mo from MediaFileOwner mo where mo.nodeId = ?1 and mo.ownerName = ?2 and mo.mediaFile.id = ?3")
    Optional<MediaFileOwner> findByFile(UUID nodeId, String ownerName, String mediaFileId);

}
