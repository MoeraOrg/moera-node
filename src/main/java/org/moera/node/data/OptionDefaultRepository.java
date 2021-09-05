package org.moera.node.data;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OptionDefaultRepository extends JpaRepository<OptionDefault, UUID> {

    @Query("select od from OptionDefault od where name = ?1")
    Optional<OptionDefault> findByName(String name);

}
