package org.moera.node.data;

import java.sql.Timestamp;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RemoteVerificationRepository extends JpaRepository<RemoteVerification, UUID> {

    @Query("delete from RemoteVerification v where v.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
