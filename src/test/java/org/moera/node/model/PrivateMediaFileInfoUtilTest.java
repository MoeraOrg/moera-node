package org.moera.node.model;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.data.MediaFile;
import org.moera.node.data.MediaFileOwner;
import org.moera.node.media.MediaOperations;

public class PrivateMediaFileInfoUtilTest {

    @Test
    void compressedMediaIdIsExposedWhenCompressionIsComplete() {
        MediaFileOwner owner = owner(true, true);
        MediaFileOwner compressedOwner = owner(false, true);
        owner.setCompressedOwner(compressedOwner);

        Assertions.assertEquals(
            compressedOwner.getId().toString(),
            PrivateMediaFileInfoUtil.build(owner, null, null).getCompressedMediaId()
        );
    }

    @Test
    void compressedMediaIdIsAbsentWhileCompressionIsIncomplete() {
        Assertions.assertNull(build(true, true).getCompressedMediaId());
    }

    @Test
    void uncompressedIsExposedOnlyForDownsizeOwnerOfUncompressedFile() {
        Assertions.assertNull(build(false, false).getUncompressed());
        Assertions.assertNull(build(false, true).getUncompressed());
        Assertions.assertNull(build(true, false).getUncompressed());
        Assertions.assertEquals(true, build(true, true).getUncompressed());
    }

    @Test
    void awaitingVideoCompressionUsesBothFlags() {
        Assertions.assertFalse(MediaOperations.awaitingVideoCompression(owner(false, false)));
        Assertions.assertFalse(MediaOperations.awaitingVideoCompression(owner(false, true)));
        Assertions.assertFalse(MediaOperations.awaitingVideoCompression(owner(true, false)));
        Assertions.assertTrue(MediaOperations.awaitingVideoCompression(owner(true, true)));
    }

    private static org.moera.lib.node.types.PrivateMediaFileInfo build(boolean uncompressed, boolean downsize) {
        return PrivateMediaFileInfoUtil.build(owner(uncompressed, downsize), null, null);
    }

    private static MediaFileOwner owner(boolean uncompressed, boolean downsize) {
        MediaFile file = new MediaFile();
        file.setId("hash");
        file.setMimeType("video/mp4");
        file.setUncompressed(uncompressed);

        MediaFileOwner owner = new MediaFileOwner();
        owner.setId(UUID.randomUUID());
        owner.setMediaFile(file);
        owner.setDownsize(downsize);
        owner.setMalwareMarks("test");
        return owner;
    }

}
