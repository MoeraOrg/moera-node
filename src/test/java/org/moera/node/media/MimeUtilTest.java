package org.moera.node.media;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MimeUtilTest {

    @Test
    void extensionUsesTikaRegistry() {
        Assertions.assertEquals("jpg", MimeUtil.extension("image/jpeg"));
        Assertions.assertEquals("png", MimeUtil.extension("image/png"));
        Assertions.assertEquals("zip", MimeUtil.extension("application/zip"));
    }

    @Test
    void extensionUsesAdditionalRegistry() {
        Assertions.assertEquals("md", MimeUtil.extension("text/markdown"));
    }

    @Test
    void unknownMimeTypeProducesDefaultExtension() {
        Assertions.assertEquals("bin", MimeUtil.extension("application/x-moera-unknown"));
        Assertions.assertEquals("txt", MimeUtil.extension("text/x-moera-unknown"));
        Assertions.assertEquals("media.jpg", MimeUtil.fileName("media", "image/jpeg"));
    }

    @Test
    void reasonableImageUsesAttachmentLimits() {
        Assertions.assertTrue(MimeUtil.isReasonableImage("image/jpeg", 2000, 1500, 5_242_880L));
        Assertions.assertFalse(MimeUtil.isReasonableImage("image/jpeg", 2000, 1500, 5_242_881L));
        Assertions.assertTrue(MimeUtil.isReasonableImage("image/png", 2000, 1500, 5_242_880L));
        Assertions.assertFalse(MimeUtil.isReasonableImage("image/png", 2000, 1500, 5_242_881L));
    }

    @Test
    void downsizeUsesJpeg() {
        Assertions.assertEquals(new MimeUtil.ThumbnailFormat("image/jpeg", "JPEG"), MimeUtil.downsize("image/jpeg"));
        Assertions.assertEquals(new MimeUtil.ThumbnailFormat("image/jpeg", "JPEG"), MimeUtil.downsize("image/png"));
    }

    @Test
    void reasonableImageForDownsizeAllowsLargeJpeg() {
        Assertions.assertFalse(MimeUtil.isReasonableImage("image/jpeg", 2000, 1500, 20_971_520L));
        Assertions.assertTrue(MimeUtil.isReasonableImageForDownsize("image/jpeg", 2000, 1500, 20_971_520L));
        Assertions.assertTrue(MimeUtil.isReasonableImageForDownsize("image/pjpeg", 2000, 1500, 20_971_520L));
        Assertions.assertFalse(MimeUtil.isReasonableImageForDownsize("image/jpeg", 2000, 1500, 20_971_521L));
    }

    @Test
    void reasonableImageForDownsizeKeepsOtherLimits() {
        Assertions.assertTrue(MimeUtil.isReasonableImageForDownsize("image/png", 2000, 1500, 5_242_880L));
        Assertions.assertFalse(MimeUtil.isReasonableImageForDownsize("image/png", 2000, 1500, 5_242_881L));
        Assertions.assertFalse(MimeUtil.isReasonableImageForDownsize("image/jpeg", 9000, 1500, 5_242_880L));
        Assertions.assertFalse(MimeUtil.isReasonableImageForDownsize("image/jpeg", 6000, 7000, 5_242_880L));
    }

}
