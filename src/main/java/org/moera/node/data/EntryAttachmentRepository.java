package org.moera.node.data;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryAttachmentRepository extends JpaRepository<EntryAttachment, UUID> {

    @Query("select count(*) from EntryAttachment ea left join ea.entryRevision er left join er.entry e"
            + " where e.nodeId = ?1 and er.entry.id = ?2 and ea.mediaFileOwner.id = ?3")
    int countByEntryIdAndMedia(UUID nodeId, UUID entryId, UUID mediaId);

    @Query("select p from EntryAttachment ea"
            + " left join ea.mediaFileOwner mfo full join mfo.postings p left join fetch p.currentRevision"
            + " where mfo.nodeId = ?1 and ea.entryRevision.id = ?2 and p.receiverName is null and p is not null"
            + " order by ea.ordinal")
    List<Posting> findOwnAttachedPostings(UUID nodeId, UUID entryRevisionId);

    @Query("select p from EntryAttachment ea"
            + " left join ea.mediaFileOwner mfo full join mfo.postings p left join fetch p.currentRevision"
            + " where mfo.nodeId = ?1 and ea.entryRevision.id = ?2 and p.receiverName = ?3 and p is not null"
            + " order by ea.ordinal")
    List<Posting> findReceivedAttachedPostings(UUID nodeId, UUID entryRevisionId, String receiverName);

}
