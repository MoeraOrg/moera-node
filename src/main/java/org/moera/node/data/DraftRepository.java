package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DraftRepository extends JpaRepository<Draft, UUID> {

    @Query("select d from Draft d"
            + " left join fetch d.attachments da left join fetch da.mediaFileOwner mfo"
            + " left join fetch mfo.mediaFile mf left join fetch mf.previews left join fetch d.ownerAvatarMediaFile"
            + " where d.nodeId = ?1 and d.draftType = org.moera.lib.node.types.DraftType.NEW_POSTING"
            + " and d.receiverName = ?2")
    List<Draft> findAllNewPosting(UUID nodeId, String nodeName, Pageable pageable);

    @Query("select d from Draft d"
            + " left join fetch d.attachments da left join fetch da.mediaFileOwner mfo"
            + " left join fetch mfo.mediaFile mf left join fetch mf.previews left join fetch d.ownerAvatarMediaFile"
            + " where d.nodeId = ?1 and d.draftType = org.moera.lib.node.types.DraftType.POSTING_UPDATE"
            + " and d.receiverName = ?2 and d.receiverPostingId = ?3")
    List<Draft> findPostingUpdate(UUID nodeId, String nodeName, String postingId, Pageable pageable);

    @Query("select d from Draft d"
            + " left join fetch d.attachments da left join fetch da.mediaFileOwner mfo"
            + " left join fetch mfo.mediaFile mf left join fetch mf.previews left join fetch d.ownerAvatarMediaFile"
            + " where d.nodeId = ?1 and d.draftType = org.moera.lib.node.types.DraftType.NEW_COMMENT"
            + " and d.receiverName = ?2 and d.receiverPostingId = ?3")
    List<Draft> findAllNewComment(UUID nodeId, String nodeName, String postingId, Pageable pageable);

    @Query("select d from Draft d"
            + " left join fetch d.attachments da left join fetch da.mediaFileOwner mfo"
            + " left join fetch mfo.mediaFile mf left join fetch mf.previews left join fetch d.ownerAvatarMediaFile"
            + " where d.nodeId = ?1 and d.draftType = org.moera.lib.node.types.DraftType.COMMENT_UPDATE"
            + " and d.receiverName = ?2 and d.receiverPostingId = ?3 and d.receiverCommentId = ?4")
    List<Draft> findCommentUpdate(UUID nodeId, String nodeName, String postingId, String commentId, Pageable pageable);

    @Query("select d from Draft d where d.nodeId = ?1 and d.id = ?2")
    Optional<Draft> findById(UUID nodeId, UUID id);

    @Query("delete from Draft d where d.deadline is not null and d.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
