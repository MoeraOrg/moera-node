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

}
