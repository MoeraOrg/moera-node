package org.moera.node.data;

import java.util.Collection;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface MediaFilePreviewRepository extends JpaRepository<MediaFilePreview, UUID> {

    @Query("select mfp from MediaFilePreview mfp where mfp.originalMediaFile.id = ?1")
    Collection<MediaFilePreview> findByOriginalId(String originalId);

}
