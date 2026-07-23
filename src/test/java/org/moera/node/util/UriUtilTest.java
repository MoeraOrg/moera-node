package org.moera.node.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class UriUtilTest {

    @Test
    void fileNameReturnsLastPathComponent() {
        Assertions.assertEquals("file.txt", UriUtil.fileName("https://example.org/media/path/file.txt"));
        Assertions.assertEquals("file.txt", UriUtil.fileName("/media/path/file.txt"));
        Assertions.assertEquals("file.txt", UriUtil.fileName("file.txt"));
    }

    @Test
    void resolveReturnsAbsoluteUri() {
        Assertions.assertEquals(
            "https://example.org/media/file.txt?download=true#details",
            UriUtil.resolve(
                "https://example.org/media/file.txt?download=true#details",
                "https://node.example.org/base/"
            )
        );
    }

    @Test
    void resolveRelativeUriAgainstBaseUri() {
        Assertions.assertEquals(
            "https://example.org/media/path/file.txt",
            UriUtil.resolve("file.txt", "https://example.org/media/path/")
        );
    }

    @Test
    void resolveRootRelativeUriAgainstBaseUri() {
        Assertions.assertEquals(
            "https://example.org/media/file.txt",
            UriUtil.resolve("/media/file.txt", "https://example.org/path/")
        );
    }

    @Test
    void resolveSchemeRelativeUriAgainstBaseUri() {
        Assertions.assertEquals(
            "https://cdn.example.org/media/file.txt",
            UriUtil.resolve("//cdn.example.org/media/file.txt", "https://example.org/path/")
        );
    }

    @Test
    void resolveAcceptsNull() {
        Assertions.assertNull(UriUtil.resolve(null, "https://example.org/path/"));
    }

    @Test
    void fileNameIgnoresQueryAndFragment() {
        Assertions.assertEquals("file.txt", UriUtil.fileName("https://example.org/media/file.txt?download=true#details"));
    }

    @Test
    void fileNameIgnoresTrailingSlash() {
        Assertions.assertEquals("path", UriUtil.fileName("https://example.org/media/path/"));
        Assertions.assertEquals("path", UriUtil.fileName("https://example.org/media/path//"));
    }

    @Test
    void fileNameAcceptsNull() {
        Assertions.assertNull(UriUtil.fileName(null));
    }

    @Test
    void queryReturnsRawQuery() {
        Assertions.assertEquals(
            "exp=1&fn=hello%20world.md",
            UriUtil.query("stored-name.legacy?exp=1&fn=hello%20world.md")
        );
    }

    @Test
    void queryAcceptsMissingQueryAndNull() {
        Assertions.assertNull(UriUtil.query("stored-name.legacy"));
        Assertions.assertNull(UriUtil.query(null));
    }

    @Test
    void stripQueryAndFragmentReturnsInitialUriPart() {
        Assertions.assertEquals(
            "stored-name.legacy",
            UriUtil.stripQueryAndFragment("stored-name.legacy?exp=1&fn=hello%20world.md")
        );
        Assertions.assertEquals(
            "stored-name.legacy",
            UriUtil.stripQueryAndFragment("stored-name.legacy?exp=1#fragment")
        );
        Assertions.assertEquals("stored-name.legacy", UriUtil.stripQueryAndFragment("stored-name.legacy#fragment"));
    }

    @Test
    void stripQueryAndFragmentAcceptsMissingSuffixAndNull() {
        Assertions.assertEquals("stored-name.legacy", UriUtil.stripQueryAndFragment("stored-name.legacy"));
        Assertions.assertNull(UriUtil.stripQueryAndFragment(null));
    }

    @Test
    void queryParameterReturnsDecodedValue() {
        Assertions.assertEquals("hello world.md", UriUtil.queryParameter("exp=1&fn=hello%20world.md", "fn"));
        Assertions.assertNull(UriUtil.queryParameter("exp=1", "fn"));
    }

    @Test
    void encodedQueryParameterReturnsEncodedValue() {
        Assertions.assertEquals("hello%20world.md", UriUtil.encodedQueryParameter("exp=1&fn=hello%20world.md", "fn"));
        Assertions.assertEquals("value", UriUtil.encodedQueryParameter("parameter%20name=value", "parameter name"));
        Assertions.assertNull(UriUtil.encodedQueryParameter("exp=1", "fn"));
    }

}
