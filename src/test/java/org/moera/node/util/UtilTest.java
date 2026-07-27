package org.moera.node.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UtilTest {

    @Test
    void startWithSlashAddsMissingSlash() {
        Assertions.assertEquals("/path", Util.startWithSlash("path"));
        Assertions.assertEquals("/path", Util.startWithSlash("/path"));
        Assertions.assertEquals("/", Util.startWithSlash(""));
        Assertions.assertEquals("//path", Util.startWithSlash("//path"));
        Assertions.assertNull(Util.startWithSlash(null));
    }

    @Test
    void startWithNoSlashRemovesLeadingSlash() {
        Assertions.assertEquals("path", Util.startWithNoSlash("/path"));
        Assertions.assertEquals("path", Util.startWithNoSlash("path"));
        Assertions.assertEquals("", Util.startWithNoSlash(""));
        Assertions.assertEquals("/path", Util.startWithNoSlash("//path"));
        Assertions.assertNull(Util.startWithNoSlash(null));
    }

    @Test
    void endWithSlashAddsMissingSlash() {
        Assertions.assertEquals("path/", Util.endWithSlash("path"));
        Assertions.assertEquals("path/", Util.endWithSlash("path/"));
        Assertions.assertEquals("/", Util.endWithSlash(""));
        Assertions.assertEquals("path//", Util.endWithSlash("path//"));
        Assertions.assertNull(Util.endWithSlash(null));
    }

    @Test
    void endWithNoSlashRemovesTrailingSlash() {
        Assertions.assertEquals("path", Util.endWithNoSlash("path/"));
        Assertions.assertEquals("path", Util.endWithNoSlash("path"));
        Assertions.assertEquals("", Util.endWithNoSlash(""));
        Assertions.assertEquals("path/", Util.endWithNoSlash("path//"));
        Assertions.assertNull(Util.endWithNoSlash(null));
    }

}
