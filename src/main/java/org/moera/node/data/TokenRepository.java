package org.moera.node.data;

import java.sql.Timestamp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface TokenRepository extends JpaRepository<Token, String> {

    @Query("delete from Token t where t.deadline < ?1")
    @Modifying
    void deleteExpired(Timestamp deadline);

}
