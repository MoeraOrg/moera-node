package org.moera.node.media;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.config.DirectServeConfig;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.util.ExtendedDuration;

public class MediaUtilTest {

    @Test
    void publicPathUsesVirtualName() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", "media-hash.legacy");

        Assertions.assertEquals("public/media-hash.md", MediaUtil.publicPath(mediaFile));
    }

    @Test
    void filesystemDirectPathUsesStoredName() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", "stored-name.legacy");
        DirectServeConfig config = directServeConfig();

        var directPath = MediaUtil.directPath(mediaFile, ExtendedDuration.ALWAYS, config);

        Assertions.assertNotNull(directPath.url());
        Assertions.assertTrue(directPath.url().startsWith("stored-name.legacy?"));
        Assertions.assertNotNull(directPath.expires());
    }

    @Test
    void filesystemDirectPathIsAbsentWithoutStoredName() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", null);

        var directPath = MediaUtil.directPath(mediaFile, ExtendedDuration.ALWAYS, directServeConfig());

        Assertions.assertNull(directPath.url());
        Assertions.assertNull(directPath.expires());
    }

    @Test
    void refreshDirectPathRebuildsUrlWithStoredNameAndUserFileName() {
        String directPath = "stored-name.legacy?exp=1&fn=hello%20world.md&sig=old";

        var refreshed = MediaUtil.refreshDirectPath(
            directPath, "media-hash", ExtendedDuration.ALWAYS, directServeConfig()
        );

        Assertions.assertTrue(refreshed.url().startsWith("stored-name.legacy?"));
        Assertions.assertTrue(refreshed.url().contains("&fn=hello%20world.md&"));
        Assertions.assertFalse(refreshed.url().contains("exp=1&"));
        Assertions.assertFalse(refreshed.url().endsWith("sig=old"));
        Assertions.assertNotNull(refreshed.expires());
    }

    @Test
    void mediaSourcesUsePreviewStoredName() {
        MediaFile original = mediaFile("original-hash", "image/png", "original.persisted");
        original.setSizeX(1200);
        MediaFile preview = mediaFile("preview-hash", "image/jpeg", "preview.persisted");
        preview.setSizeX(900);
        MediaFilePreview previewLink = new MediaFilePreview();
        previewLink.setId(UUID.randomUUID());
        previewLink.setOriginalMediaFile(original);
        previewLink.setMediaFile(preview);
        previewLink.setWidth(900);
        original.getPreviews().add(previewLink);
        MediaFileOwner owner = new MediaFileOwner();
        owner.setId(UUID.randomUUID());
        owner.setMediaFile(original);

        String sources = MediaUtil.mediaSources("/original", owner, directServeConfig());

        Assertions.assertTrue(sources.contains("preview.persisted?"));
        Assertions.assertFalse(sources.contains("original.persisted?"));
    }

    private static MediaFile mediaFile(String id, String mimeType, String fileName) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(id);
        mediaFile.setMimeType(mimeType);
        mediaFile.setFileName(fileName);
        return mediaFile;
    }

    private static DirectServeConfig directServeConfig() {
        DirectServeConfig config = new DirectServeConfig();
        config.setSource(DirectServeSource.FILESYSTEM);
        config.setSecret("secret");
        return config;
    }

}
