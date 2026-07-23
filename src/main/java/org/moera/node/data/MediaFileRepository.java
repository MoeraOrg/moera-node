package org.moera.node.data;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface MediaFileRepository extends JpaRepository<MediaFile, String> {

    @Query(value = "select pg_advisory_xact_lock(hashtextextended('media-file:' || ?1, 0))", nativeQuery = true)
    void lockMediaFileId(String id);

    @Query("select count(mf) from MediaFile mf where mf.id = ?1")
    long countById(String id);

    @Query(
        value = "with candidates as ("
            + " select id from media_files"
            + " where deadline is not null and deadline < ?1"
            + " order by id limit ?2 for update skip locked"
            + "),"
            + " removed as ("
            + " delete from media_files mf using candidates c where mf.id = c.id"
            + " returning mf.id, mf.file_name, mf.cloud_file_name"
            + ")"
            + " insert into media_file_removals(id, media_file_id, file_name, cloud_file_name, created_at)"
            + " select nextval('media_file_removals_seq'), id, file_name, cloud_file_name, now() from removed",
        nativeQuery = true
    )
    @Modifying
    int moveUnusedToRemovals(Timestamp deadline, int limit);

    @Query("select mf from MediaFile mf where mf.digest is null and mf.fileName is not null")
    List<MediaFile> findWithNoDigest(Pageable pageable);

    @Query("select count(*) from MediaFile mf where mf.id like '%='")
    int countIdWithPadding();

    @Query("select mf from MediaFile mf where mf.id like '%='")
    Page<MediaFile> findIdWithPadding(Pageable pageable);

    @Modifying
    @Query("update MediaFile mf set mf.id = ?2, mf.fileName = ?3 where mf.id = ?1")
    void updateId(String oldId, String newId, String fileName);

    @Query("select mf from MediaFile mf where mf.exposed = true")
    Page<MediaFile> findAllExposed(Pageable pageable);

    @Query("select mf.id from MediaFile mf where mf.recognizeAt <= ?1 and mf.recognizedAt is null")
    List<String> findToRecognize(Timestamp now);

    @Query("update MediaFile mf set mf.recognizeAt = ?2 where mf.id = ?1")
    @Modifying
    void assignRecognizeAt(String id, Timestamp recognizeAt);

    @Query("update MediaFile mf set mf.recognizedText = ?2, mf.recognizedAt = ?3 where mf.id = ?1")
    @Modifying
    void recognized(String id, String recognizedText, Timestamp recognizedAt);

}
