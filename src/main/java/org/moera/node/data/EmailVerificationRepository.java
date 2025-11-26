package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    @Query("select v from EmailVerification v where v.token = ?1 and v.deadline >= ?2")
    Optional<EmailVerification> findByToken(String token, Timestamp now);

    @Modifying
    @Query("delete from EmailVerification v where v.deadline < ?1")
    void deleteExpired(Timestamp now);

}
