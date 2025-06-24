package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InitialRecommendationRepository extends JpaRepository<InitialRecommendation, UUID> {

    @Query("select ir from InitialRecommendation ir where ir.nodeName = ?1 and ir.postingId = ?2")
    Optional<InitialRecommendation> findByNodeNameAndPostingId(String nodeName, String postingId);

    @Query("delete from InitialRecommendation ir where ir.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp now);

}
