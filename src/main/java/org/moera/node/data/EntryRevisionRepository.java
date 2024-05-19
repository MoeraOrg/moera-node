package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EntryRevisionRepository extends JpaRepository<EntryRevision, UUID> {

    String CONDITION_UNUSED = "r.deletedAt is not null"
            + " and not exists(select rp from EntryRevision rp where rp.parent.id = r.id)"
            + " and not exists(select rl from Entry rl where rl.repliedToRevision.id = r.id)"
            + " and not exists(select ra from Reaction ra where ra.entryRevision.id = r.id)"
            + " and not exists(select rt from ReactionTotal rt where rt.entryRevision.id = r.id)";

    @Query("select r from EntryRevision r"
            + " where r.entry.nodeId = ?1 and r.entry.id = ?2 and r.entry.deletedAt is null and r.id = ?3")
    Optional<EntryRevision> findByEntryIdAndId(UUID nodeId, UUID entryId, UUID id);

    @Query("select r from EntryRevision r"
            + " left join fetch r.attachments ra left join fetch ra.mediaFileOwner mfo"
            + " left join fetch mfo.mediaFile mf left join fetch mf.previews"
            + " where r.entry.nodeId = ?1 and r.entry.id = ?2 and r.entry.deletedAt is null and r.id = ?3")
    Optional<EntryRevision> findWithAttachmentsByEntryIdAndId(UUID nodeId, UUID entryId, UUID id);

    @Query("select r from EntryRevision r"
            + " where r.entry.nodeId = ?1 and r.entry.id = ?2 and r.entry.deletedAt is not null and r.id = ?3")
    Optional<EntryRevision> findByDeletedEntryIdAndId(UUID nodeId, UUID entryId, UUID id);

    @Query("select r from EntryRevision r"
            + " left join fetch r.attachments ra left join fetch ra.mediaFileOwner mfo"
            + " left join fetch mfo.mediaFile mf left join fetch mf.previews"
            + " where r.entry.nodeId = ?1 and r.entry.id = ?2 and r.entry.deletedAt is not null and r.id = ?3")
    Optional<EntryRevision> findWithAttachmentsByDeletedEntryIdAndId(UUID nodeId, UUID entryId, UUID id);

    @Query("select r from EntryRevision r where r.entry.nodeId = ?1 and r.entry.id = ?2")
    Page<EntryRevision> findAllByEntryId(UUID nodeId, UUID entryId, Pageable pageable);

    @Query("select r.entry.id from EntryRevision r where r.entry.nodeId = ?1 and r.entry.entryType = ?2"
            + " and r.entry.receiverName is null and r.createdAt < ?3 and " + CONDITION_UNUSED)
    Set<UUID> findOriginalEntriesWithOutdated(UUID nodeId, EntryType entryType, Timestamp createdBefore);

    @Query("select r.entry.id from EntryRevision r where r.entry.nodeId = ?1 and r.entry.entryType = ?2"
            + " and r.entry.receiverName is not null and r.createdAt < ?3 and " + CONDITION_UNUSED)
    Set<UUID> findNotOriginalEntriesWithOutdated(UUID nodeId, EntryType entryType, Timestamp createdBefore);

    @Modifying
    @Query("delete EntryRevision r where r.entry.id = ?1 and r.createdAt < ?2 and " + CONDITION_UNUSED)
    void deleteOutdated(UUID entryId, Timestamp createdBefore);

    @Modifying
    @Query("update Entry e set e.totalRevisions = (select count(*) from EntryRevision r where r.entry.id = e.id)"
            + " where e.id = ?1")
    void updateTotalRevisions(UUID entryId);

    @Modifying
    @Query("update EntryRevision r set r.attachmentsCache = null"
            + " where exists("
            + "select ea from EntryAttachment ea where ea.entryRevision.id = r.id and ea.mediaFileOwner = ?1"
            + ")")
    void clearAttachmentsCache(UUID mediaFileOwnerId);

}
