package org.moera.node.media;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MimeUtilsTest {

    @Test
    void extensionUsesTikaRegistry() {
        Assertions.assertEquals("jpg", MimeUtils.extension("image/jpeg"));
        Assertions.assertEquals("png", MimeUtils.extension("image/png"));
        Assertions.assertEquals("zip", MimeUtils.extension("application/zip"));
    }

    @Test
    void unknownMimeTypeProducesEmptyExtension() {
        Assertions.assertEquals("", MimeUtils.extension("application/x-moera-unknown"));
        Assertions.assertEquals("media.jpg", MimeUtils.fileName("media", "image/jpeg"));
    }

}
