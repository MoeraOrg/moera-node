package org.moera.node.data;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaFileRemovalRepository extends JpaRepository<MediaFileRemoval, Long> {

    @Query("select r.id from MediaFileRemoval r order by r.id")
    List<Long> findPendingIds(Pageable pageable);

    @Query("delete from MediaFileRemoval r where r.id = ?1")
    @Modifying
    void deleteById(long id);

}
