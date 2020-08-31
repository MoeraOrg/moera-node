package org.moera.node.text.markdown.spoiler;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.test.util.TestUtils;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;

class SpoilerTest {
    final private static DataHolder OPTIONS = new MutableDataSet()
            .set(TestUtils.NO_FILE_EOL, false)
            .set(Parser.EXTENSIONS, Collections.singleton(SpoilerExtension.create()));
    final private static @NotNull Parser PARSER = Parser.builder(OPTIONS).build();
    final private static @NotNull HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();

    private void assertRendering(String input, String expectedResult) {
        assertEquals(expectedResult, RENDERER.render(PARSER.parse(input)));
    }

    @Test
    public void onePipeIsNotEnough() {
        assertRendering("|foo|", "<p>|foo|</p>\n");
    }

    @Test
    public void twoPipesYay() {
        assertRendering("||foo||", "<p><mr-spoiler>foo</mr-spoiler></p>\n");
    }

    @Test
    public void fourPipesNope() {
        assertRendering("foo ||||", "<p>foo ||||</p>\n");
    }

    @Test
    public void unmatched() {
        assertRendering("||foo", "<p>||foo</p>\n");
        assertRendering("foo||", "<p>foo||</p>\n");
    }

    @Test
    public void threeInnerThree() {
        assertRendering("|||foo|||", "<p>|<mr-spoiler>foo</mr-spoiler>|</p>\n");
    }

    @Test
    public void twoInnerThree() {
        assertRendering("||foo|||", "<p><mr-spoiler>foo</mr-spoiler>|</p>\n");
    }

    @Test
    public void tildesInside() {
        assertRendering("||foo|bar||", "<p><mr-spoiler>foo|bar</mr-spoiler></p>\n");
        assertRendering("||foo||bar||", "<p><mr-spoiler>foo</mr-spoiler>bar||</p>\n");
        assertRendering("||foo|||bar||", "<p><mr-spoiler>foo</mr-spoiler>|bar||</p>\n");
        assertRendering("||foo||||bar||", "<p><mr-spoiler>foo</mr-spoiler><mr-spoiler>bar</mr-spoiler></p>\n");
        assertRendering("||foo|||||bar||", "<p><mr-spoiler>foo</mr-spoiler>|<mr-spoiler>bar</mr-spoiler></p>\n");
        assertRendering("||foo||||||bar||", "<p><mr-spoiler>foo</mr-spoiler>||<mr-spoiler>bar</mr-spoiler></p>\n");
        assertRendering("||foo|||||||bar||", "<p><mr-spoiler>foo</mr-spoiler>|||<mr-spoiler>bar</mr-spoiler></p>\n");
    }

    @Test
    public void strikethroughWholeParagraphWithOtherDelimiters() {
        assertRendering("||Paragraph with *emphasis* and __strong emphasis__||", "<p><mr-spoiler>Paragraph with <em>emphasis</em> and <strong>strong emphasis</strong></mr-spoiler></p>\n");
    }

    @Test
    public void insideBlockQuote() {
        assertRendering("> strike ||that||", "<blockquote>\n<p>strike <mr-spoiler>that</mr-spoiler></p>\n</blockquote>\n");
    }

    @Test
    public void delimited() {
        Node document = PARSER.parse("||foo||");
        Spoiler strikethrough = (Spoiler) document.getFirstChild().getFirstChild();
        assertEquals("||", strikethrough.getOpeningMarker().toString());
        assertEquals("||", strikethrough.getClosingMarker().toString());
    }
}