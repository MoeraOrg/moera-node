package org.moera.node.text.markdown.mention;

import java.util.List;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.test.util.TestUtils;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.moera.node.text.markdown.spoiler.SpoilerExtension;

class MentionsTest {

    private static final DataHolder OPTIONS = new MutableDataSet()
            .set(TestUtils.NO_FILE_EOL, false)
            .set(Parser.EXTENSIONS, List.of(MentionsExtension.create(), SpoilerExtension.create()));
    private static final Parser PARSER = Parser.builder(OPTIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();


    private void assertRendering(String input, String expectedResult) {
        Assertions.assertEquals(expectedResult, RENDERER.render(PARSER.parse(input)));
    }

    @Test
    void onlyMention() {
        String expectedResult = "<p>" +
                "<a href=\"https://moera.page/@simple-username/~/\" data-nodename=\"simple-username_0\">" +
                "@simple-username" +
                "</a>" +
                "</p>\n";
        assertRendering("@simple-username", expectedResult);
    }

    @Test
    void mentionInAWord() {
        assertRendering("not so@simple-username", "<p>not so@simple-username</p>\n");
    }

    @Test
    void escapedMention() {
        assertRendering("not so \\@simple-username", "<p>not so @simple-username</p>\n");
    }

    @Test
    void mentionInASentence() {
        String expectedResult = "<p>not so " +
                "<a href=\"https://moera.page/@simple-username/~/\" data-nodename=\"simple-username_0\">" +
                "@simple-username" +
                "</a>" +
                " after all</p>\n";
        assertRendering("not so @simple-username after all", expectedResult);
    }

    @Test
    void mentionAfterSpoiler() {
        String expectedResult = "<p>" +
                "<mr-spoiler>not so</mr-spoiler>" +
                "<a href=\"https://moera.page/@simple-username/~/\" data-nodename=\"simple-username_0\">" +
                "@simple-username" +
                "</a>" +
                " after all</p>\n";
        assertRendering("||not so||@simple-username after all", expectedResult);
    }

    @Test
    void weirdMentionInASentence() {
        String expectedResult = "<p>very " +
                "<a " +
                "href=\"https://moera.page/@-!!.weird**UsErNamEE--/~/\" " +
                "data-nodename=\"-!!.weird**UsErNamEE--_0\">" +
                "@-!!.weird**UsErNamEE--" +
                "</a>" +
                " indeed</p>\n";
        assertRendering("very @-!!.weird**UsErNamEE-- indeed", expectedResult);
    }

    @Test
    void mentionWithPunctuation() {
        String expectedResult = "<p>mention just " +
                "<a href=\"https://moera.page/@username/~/\" data-nodename=\"username_0\">" +
                "@username" +
                "</a>" +
                ".</p>\n";
        assertRendering("mention just @username.", expectedResult);
    }

    @Test
    void mentionWithGeneration() {
        String expectedResult = "<p>only one generation of " +
                "<a href=\"https://moera.page/@username_123/~/\" data-nodename=\"username_123\">" +
                "@username_123" +
                "</a>" +
                ".</p>\n";
        assertRendering("only one generation of @username_123.", expectedResult);
    }

}
