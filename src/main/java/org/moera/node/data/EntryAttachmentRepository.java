package org.moera.node.data;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntryAttachmentRepository extends JpaRepository<EntryAttachment, UUID> {

    @Query(
        "select ea from EntryAttachment ea"
        + " left join fetch ea.mediaFileOwner mfo left join fetch mfo.mediaFile mf left join fetch mf.previews mfp"
        + " left join fetch mfp.mediaFile left join fetch mfo.postings"
        + " where ea.entryRevision.id = ?1"
    )
    Set<EntryAttachment> findByEntryRevision(UUID entryRevisionId);

    @Query(
        "select count(*) from EntryAttachment ea left join ea.entryRevision er left join er.entry e"
        + " where e.nodeId = ?1 and er.entry.id = ?2 and ea.mediaFileOwner.id = ?3"
    )
    int countByEntryIdAndMedia(UUID nodeId, UUID entryId, UUID mediaId);

    @Query(
        "select er.entry from EntryAttachment ea full join ea.entryRevision er"
        + " where er.deletedAt is null and ea.mediaFileOwner.nodeId = ?1 and ea.mediaFileOwner.id = ?2"
    )
    Set<Entry> findEntriesByMedia(UUID nodeId, UUID mediaId);

    @Query(
        "select ea.draft from EntryAttachment ea"
        + " where ea.draft is not null and ea.mediaFileOwner.nodeId = ?1 and ea.mediaFileOwner.id = ?2"
    )
    Set<Draft> findDraftsByMedia(UUID nodeId, UUID mediaId);

    @Query(
        "select p from EntryAttachment ea"
        + " left join ea.mediaFileOwner mfo full join mfo.postings p left join fetch p.currentRevision"
        + " where mfo.nodeId = ?1 and ea.entryRevision.id = ?2 and p.receiverName is null and p is not null"
        + " order by ea.ordinal"
    )
    List<Posting> findOwnAttachedPostings(UUID nodeId, UUID entryRevisionId);

    @Query(
        "select p from EntryAttachment ea"
        + " left join ea.mediaFileOwner mfo full join mfo.postings p left join fetch p.currentRevision"
        + " where mfo.nodeId = ?1 and ea.entryRevision.id = ?2 and p.receiverName = ?3 and p is not null"
        + " order by ea.ordinal"
    )
    List<Posting> findReceivedAttachedPostings(UUID nodeId, UUID entryRevisionId, String receiverName);

}
