package org.moera.node.media;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.config.Config;
import org.moera.node.config.DirectServeSource;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.data.MediaFilePreview;
import org.moera.node.domain.Domains;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.ExtendedDuration;
import org.springframework.test.util.ReflectionTestUtils;

public class MediaUtilTest {

    @Test
    void publicPathUsesVirtualName() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", "media-hash.legacy");

        Assertions.assertEquals("public/media-hash.md", MediaUtil.publicPath(mediaFile));
    }

    @Test
    void filesystemDirectPathUsesStoredName() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", "stored-name.legacy");
        DirectServeOperations directServeOperations = directServeOperations();

        var directPath = directServeOperations.directPath(mediaFile, ExtendedDuration.ALWAYS);

        Assertions.assertNotNull(directPath.url());
        Assertions.assertTrue(directPath.url().startsWith("stored-name.legacy?"));
        Assertions.assertNotNull(directPath.expires());
    }

    @Test
    void filesystemDirectPathIsAbsentWithoutStoredName() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", null);

        var directPath = directServeOperations().directPath(mediaFile, ExtendedDuration.ALWAYS);

        Assertions.assertNull(directPath.url());
        Assertions.assertNull(directPath.expires());
    }

    @Test
    void filesystemDirectUrlIsAbsolute() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", "stored-name.legacy");

        String directUrl = directServeOperations().directUrl(mediaFile);

        Assertions.assertTrue(directUrl.startsWith(
            "https://node.example/moera/media/stored-name.legacy?"
        ));
    }

    @Test
    void absoluteDirectUrlIsPreserved() {
        String url = "https://media.example/media.jpg?signature=value";
        DirectServeOperations directServeOperations = new DirectServeOperations() {
            @Override
            public DirectServePath directPath(MediaFile mediaFile, ExtendedDuration valid) {
                return new DirectServePath(url, 1L);
            }
        };

        Assertions.assertEquals(url, directServeOperations.directUrl(new MediaFile()));
    }

    @Test
    void refreshDirectPathRebuildsUrlWithStoredNameAndUserFileName() {
        String directPath = "stored-name.legacy?exp=1&fn=hello%20world.md&sig=old";

        var refreshed = directServeOperations().refreshDirectPath(
            directPath, "media-hash", ExtendedDuration.ALWAYS
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

        String sources = MediaUtil.mediaSources("/original", owner, directServeOperations());

        Assertions.assertTrue(sources.contains("preview.persisted?"));
        Assertions.assertFalse(sources.contains("original.persisted?"));
    }

    @Test
    void filesystemDirectDownloadPathIsBuiltServerSide() {
        MediaFile mediaFile = mediaFile("media-hash", "text/markdown", "stored-name.legacy");

        var directPath = directServeOperations().directDownloadPath(
            mediaFile, ExtendedDuration.ALWAYS, "notes.md"
        );

        Assertions.assertNotNull(directPath.url());
        Assertions.assertTrue(directPath.url().startsWith("stored-name.legacy?"));
        Assertions.assertTrue(directPath.url().contains("&fn=notes.md&"));
        Assertions.assertTrue(directPath.url().endsWith("&download=true"));
    }

    @Test
    void mediaUrlPrefixesRelativePath() {
        Assertions.assertEquals(
            "/moera/media/private/media.jpg?grant=value",
            MediaUtil.mediaUrl("private/media.jpg?grant=value")
        );
    }

    @Test
    void mediaUrlPreservesAbsoluteUrl() {
        String url = "https://media.example/media.jpg?signature=value";

        Assertions.assertEquals(url, MediaUtil.mediaUrl(url));
    }

    private static MediaFile mediaFile(String id, String mimeType, String fileName) {
        MediaFile mediaFile = new MediaFile();
        mediaFile.setId(id);
        mediaFile.setMimeType(mimeType);
        mediaFile.setFileName(fileName);
        return mediaFile;
    }

    private static DirectServeOperations directServeOperations() {
        Config config = new Config();
        config.getMedia().getDirectServe().setSource(DirectServeSource.FILESYSTEM);
        config.getMedia().getDirectServe().setSecret("secret");
        DirectServeOperations directServeOperations = new DirectServeOperations();
        ReflectionTestUtils.setField(directServeOperations, "config", config);
        ReflectionTestUtils.setField(directServeOperations, "domains", new Domains() {
            @Override
            public String getDomainDnsName(UUID nodeId) {
                return "node.example";
            }
        });
        ReflectionTestUtils.setField(directServeOperations, "universalContext", new UniversalContext() {
            @Override
            public UUID nodeId() {
                return UUID.fromString("12345678-1234-1234-1234-123456789abc");
            }
        });
        return directServeOperations;
    }

}
