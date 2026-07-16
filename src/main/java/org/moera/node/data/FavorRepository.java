package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface FavorRepository extends JpaRepository<Favor, UUID> {

    @Query("select f from Favor f where f.nodeId = ?1 and f.nodeName = ?2")
    Collection<Favor> findByNodeName(UUID nodeId, String nodeName);

    @Query(
        "select count(f) from Favor f"
        + " where f.nodeId = ?1 and f.nodeName = ?2 and f.favorType = ?3 and f.deadline >= ?4"
    )
    int countByNodeNameAndFavorType(UUID nodeId, String nodeName, FavorType favorType, Timestamp deadline);

    @Query("delete from Favor f where f.nodeId = ?1 and f.nodeName = ?2 and f.favorType = ?3")
    @Modifying
    void deleteByNodeNameAndFavorType(UUID nodeId, String nodeName, FavorType favorType);

    @Query("delete from Favor f where f.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
