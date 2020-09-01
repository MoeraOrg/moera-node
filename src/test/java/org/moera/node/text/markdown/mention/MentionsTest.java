package org.moera.node.text.markdown.mention;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.test.util.TestUtils;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.junit.jupiter.api.Test;
import org.moera.node.text.markdown.spoiler.SpoilerExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

class MentionsTest {

    private static final DataHolder OPTIONS = new MutableDataSet()
            .set(TestUtils.NO_FILE_EOL, false)
            .set(Parser.EXTENSIONS, List.of(MentionsExtension.create(), SpoilerExtension.create()));
    private static final Parser PARSER = Parser.builder(OPTIONS).build();
    private static final HtmlRenderer RENDERER = HtmlRenderer.builder(OPTIONS).build();


    private void assertRendering(String input, String expectedResult) {
        assertEquals(expectedResult, RENDERER.render(PARSER.parse(input)));
    }

    @Test
    void onlyMention() {
        String expectedResult = "<p>" +
                "<a href=\"/moera/gotoname?name=simple-username\" data-nodename=\"simple-username\">" +
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
    void mentionInASentence() {
        String expectedResult = "<p>not so " +
                "<a href=\"/moera/gotoname?name=simple-username\" data-nodename=\"simple-username\">" +
                "@simple-username" +
                "</a>" +
                " after all</p>\n";
        assertRendering("not so @simple-username after all", expectedResult);
    }

    @Test
    void mentionAfterSpoiler() {
        String expectedResult = "<p>" +
                "<mr-spoiler>not so</mr-spoiler>" +
                "<a href=\"/moera/gotoname?name=simple-username\" data-nodename=\"simple-username\">" +
                "@simple-username" +
                "</a>" +
                " after all</p>\n";
        assertRendering("||not so||@simple-username after all", expectedResult);
    }

    @Test
    void weirdMentionInASentence() {
        String expectedResult = "<p>very " +
                "<a " +
                "href=\"/moera/gotoname?name=-%3D%21%21.%3F%3Fweird**UsErNamEE%2B--\" " +
                "data-nodename=\"-=!!.??weird**UsErNamEE+--\">" +
                "@-=!!.??weird**UsErNamEE+--" +
                "</a>" +
                " indeed</p>\n";
        assertRendering("very @-=!!.??weird**UsErNamEE+-- indeed", expectedResult);
    }

}