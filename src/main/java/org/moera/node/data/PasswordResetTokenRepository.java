package org.moera.node.data;

import java.sql.Timestamp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    @Query("delete from PasswordResetToken t where t.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
