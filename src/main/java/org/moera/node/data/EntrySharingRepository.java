package org.moera.node.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.moera.lib.node.types.SharingType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface EntrySharingRepository extends JpaRepository<EntrySharing, UUID> {

    @Query("select s from EntrySharing s where s.entry.id = ?1 and s.id = ?2")
    Optional<EntrySharing> findByEntryIdAndId(UUID entryId, UUID id);

    @Query(
        "select s from EntrySharing s left join fetch s.remoteAvatarMediaFile"
        + " where s.entry.id = ?1 order by s.moment"
    )
    List<EntrySharing> findByEntryId(UUID entryId);

    @Query("select count(s) from EntrySharing s where s.entry.id = ?1")
    int countByEntryId(UUID entryId);

    @Query("select count(s) from EntrySharing s where s.entry.id = ?1 and s.moment = ?2")
    int countMoments(UUID entryId, long moment);

    @Query(
        "select s from EntrySharing s left join fetch s.remoteAvatarMediaFile"
        + " where s.entry.id = ?1 and s.moment > ?2 and s.moment <= ?3 order by s.moment desc"
    )
    List<EntrySharing> findSlice(UUID entryId, long afterMoment, long beforeMoment, Pageable pageable);

    @Query(
        "select s from EntrySharing s left join s.entryRevision r"
        + " where s.entry.id = ?1 and s.sharingType = ?2 and s.remoteNodeName = ?3"
        + " and (r.id = ?4 or r.id is null and ?4 is null)"
        + " and (s.remoteStoryId = ?5 or s.remoteStoryId is null and ?5 is null)"
        + " and (s.remotePostingId = ?6 or s.remotePostingId is null and ?6 is null)"
    )
    List<EntrySharing> findMatching(
        UUID entryId, SharingType sharingType, String remoteNodeName, UUID entryRevisionId, String remoteStoryId,
        String remotePostingId
    );

}
