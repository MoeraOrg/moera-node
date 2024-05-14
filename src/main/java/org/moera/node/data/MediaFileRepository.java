package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaFileRepository extends JpaRepository<MediaFile, String> {

    @Query("select mf from MediaFile mf where mf.deadline is not null and mf.deadline < ?1")
    Collection<MediaFile> findUnused(Timestamp deadline);

    @Query("delete from MediaFile mf where mf.deadline is not null and mf.deadline < ?1")
    @Modifying
    void deleteUnused(Timestamp deadline);

    @Query("select mf from MediaFile mf where mf.digest is null")
    List<MediaFile> findWithNoDigest(Pageable pageable);

    @Query("select count(*) from MediaFile mf where mf.id like '%='")
    int countIdWithPadding();

    @Query("select mf from MediaFile mf where mf.id like '%='")
    Page<MediaFile> findIdWithPadding(Pageable pageable);

    @Modifying
    @Query("update MediaFile mf set mf.id = ?2 where mf.id = ?1")
    void updateId(String oldId, String newId);

}
