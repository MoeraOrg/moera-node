package org.moera.node.data;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFilePreviewRepository extends JpaRepository<MediaFilePreview, UUID> {
}
