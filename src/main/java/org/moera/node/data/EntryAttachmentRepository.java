package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryAttachmentRepository extends JpaRepository<EntryAttachment, UUID> {

    @Query("select count(*) from EntryAttachment ea left join ea.entryRevision er left join er.entry e"
            + " where e.nodeId = ?1 and er.entry.id = ?2 and ea.mediaFileOwner.id = ?3")
    int countByEntryIdAndMedia(UUID nodeId, UUID entryId, UUID mediaId);

}
