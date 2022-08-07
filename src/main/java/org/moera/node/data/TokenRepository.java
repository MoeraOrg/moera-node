package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TokenRepository extends JpaRepository<Token, UUID> {

    @Query("select t from Token t where t.nodeId = ?1 and t.id = ?2 and (t.deadline is null or t.deadline >= ?3)")
    Optional<Token> findByNodeIdAndId(UUID nodeId, UUID id, Timestamp deadline);

    @Query("select t from Token t where t.nodeId = ?1 and t.token = ?2 and (t.deadline is null or t.deadline >= ?3)")
    Optional<Token> findByToken(UUID nodeId, String token, Timestamp deadline);

    @Query("select t from Token t where t.nodeId = ?1 and (t.deadline is null or t.deadline >= ?2)"
            + " order by t.createdAt desc")
    List<Token> findAllByNodeId(UUID nodeId, Timestamp deadline);

    @Query("delete from Token t where t.deadline is not null and t.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
