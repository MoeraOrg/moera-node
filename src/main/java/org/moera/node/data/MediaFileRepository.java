package org.moera.node.data;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.List;

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

}
