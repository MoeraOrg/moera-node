package org.moera.node.text;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.moera.lib.node.types.body.Body;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostingReferencesExtractorTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "https://moera.page/@alice/~/post/abc",
        "HTTP://WEB.MOERA.ORG/@alice/alice.example/post/abc",
        "https://moera.page/@alice/http:alice.example:8080/post/abc",
        "https://moera.page/@alice/~/post/abc/?utm_source=feed#heading",
        "https://moera.page/@alice/~/post/abc?foo=a*b&bar=$value&baz=[value]",
        "https://moera.page/@alice/~/post/abc?foo=%26comment%3Ddef",
        "https://moera.page/@alice/~/post/abc?foo=123&amp;amp;comment=def",
        "https://moera.page/&#64;alice/~/post/&#97;bc",
        "https://moera.page/@alice%5F0/~/post/%61bc"
    })
    void extractsPostingLinksFromAnchorHrefs(String url) {
        assertEquals(Set.of(new PostingReferencesExtractor.Reference("alice_0", "abc")),
            extract("<p>See <a href=\"" + url + "\"><strong>this post</strong></a>.</p>"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://example.org/@alice/~/post/abc",
        "https://moera.page.example.org/@alice/~/post/abc",
        "https://moera.page@example.org/@alice/~/post/abc",
        "https://moera.page/@alice/~/post/abc?comment=def",
        "https://web.moera.org/@alice/~/post/abc?before=123&comment=def",
        "https://moera.page/@alice/~/post/abc?foo=a*b&comment=def",
        "https://moera.page/@alice/~/post/abc?foo=$value&comment=def",
        "https://moera.page/@alice/~/post/abc?foo=[value]&comment=def",
        "https://moera.page/@alice/~/post/abc?foo=a'b&comment=def",
        "https://moera.page/@alice/~/post/abc?foo=a*b&media=def",
        "https://moera.page/@alice/~/post/abc?before=123&amp;comment=def",
        "https://moera.page/@alice/~/post/abc?before=123&#38;comment=def",
        "https://moera.page/@alice/~/post/abc?%63omment=def",
        "https://moera.page/@alice/~/post/abc?comment",
        "https://moera.page/@alice/~/post/abc?media=def",
        "https://moera.page/@alice/~/post/abc/comment/def",
        "https://moera.page/@alice/~/post/abc/media/def",
        "https://moera.page/@alice/~/profile",
        "https://moera.page/@alice/~/post/",
        "https://moera.page/@/alice.example/post/abc",
        "https://moera.page/@:/~/post/abc",
        "https://moera.page/@alice_bad/~/post/abc",
        "https://moera.page/@alice/~/post/%xx",
        "https://moera.page",
        "ftp://moera.page/@alice/~/post/abc",
        "javascript:https://moera.page/@alice/~/post/abc",
        "//moera.page/@alice/~/post/abc",
        "",
        "#heading"
    })
    void ignoresOtherTargetsAndMalformedUrls(String url) {
        assertTrue(extract("<a href=\"" + url + "\">a link</a>").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "https://moera.page/@alice/~/post/abc",
        "[a post](https://moera.page/@alice/~/post/abc)",
        "<code>https://moera.page/@alice/~/post/abc</code>",
        "<!-- <a href=\"https://moera.page/@alice/~/post/abc\">a post</a> -->",
        "<script>const url = 'https://moera.page/@alice/~/post/abc';</script>",
        "<img src=\"https://moera.page/@alice/~/post/abc\">",
        "<div href=\"https://moera.page/@alice/~/post/abc\">a post</div>",
        "<a title=\"https://moera.page/@alice/~/post/abc\">a post</a>",
        "<a href=\"https://example.org\">https://moera.page/@alice/~/post/abc</a>",
        "&lt;a href=\"https://moera.page/@alice/~/post/abc\"&gt;a post&lt;/a&gt;"
    })
    void ignoresUrlsOutsideAnchorHrefs(String html) {
        assertTrue(extract(html).isEmpty());
    }

    @Test
    void deduplicatesNodeAndPostingAcrossUrlSpellings() {
        assertEquals(Set.of(
            new PostingReferencesExtractor.Reference("alice_0", "abc"),
            new PostingReferencesExtractor.Reference("alice_1", "abc"),
            new PostingReferencesExtractor.Reference("alice_0", "def")
        ), extract("""
            <a href="https://moera.page/@alice/~/post/abc">first</a>
            <a href="https://web.moera.org/@alice_0/alice.example/post/abc">first again</a>
            <a href="https://moera.page/@alice_1/~/post/abc">another node</a>
            <a href="https://moera.page/@alice/~/post/def">another post</a>
            """));
    }

    @Test
    void extractsAdjacentAnchorsSeparately() {
        assertEquals(Set.of(
            new PostingReferencesExtractor.Reference("alice_0", "abc"),
            new PostingReferencesExtractor.Reference("alice_0", "def")
        ), extract("""
            <a href="https://moera.page/@alice/~/post/abc">first</a><a href="https://moera.page/@alice/~/post/def">second</a>
            """));
    }

    @Test
    void ignoresSubjectAndToleratesMissingText() {
        Body body = new Body();
        assertTrue(PostingReferencesExtractor.extract(body).isEmpty());
        body.setSubject("<a href=\"https://moera.page/@alice/~/post/abc\">a post</a>");
        assertTrue(PostingReferencesExtractor.extract(body).isEmpty());
        body.setText("<p>No references</p>");
        assertTrue(PostingReferencesExtractor.extract(body).isEmpty());
    }

    @Test
    void malformedCandidateDoesNotHideTheNextLink() {
        assertEquals(Set.of(new PostingReferencesExtractor.Reference("alice_0", "abc")),
            extract("""
                <a href="https://moera.page/@alice_bad/~/post/abc">invalid</a>
                <a href="https://moera.page/@alice/~/post/abc">valid</a>
                """));
    }

    private Set<PostingReferencesExtractor.Reference> extract(String text) {
        Body body = new Body();
        body.setText(text);
        return PostingReferencesExtractor.extract(body);
    }

}
