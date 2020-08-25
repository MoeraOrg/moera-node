package org.moera.node.text;

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.moera.node.test.HtmlMatcher;


class HtmlProcessorTest {

    @Test
    public void checkSimpleSpoiler() {
        @Language("HTML")
        String source = "I will not tell you that killer is <spoiler>gardener</spoiler>, it's just unethical.";
        @Language("HTML")
        String expected = "I will not tell you that killer is " +
                "<details class=\"spoiler\">\n" +
                "    <summary>spoiler!</summary>gardener\n" +
                "</details>, it's just unethical.";

        String actual = HtmlProcessor.process(source);

        HtmlMatcher.assertEqHtml(expected, actual);
    }

    @Test
    public void checkSpoilerWithSummary() {
        @Language("HTML")
        String source = "I will not tell you that killer is " +
                "<spoiler><summary>big shock</summary>gardener</spoiler>, it's just unethical.";
        @Language("HTML")
        String expected = "I will not tell you that killer is " +
                "<details class=\"spoiler\">\n" +
                "    <summary>big shock</summary>gardener\n" +
                "</details>, it's just unethical.";

        String actual = HtmlProcessor.process(source);

        HtmlMatcher.assertEqHtml(expected, actual);
    }
}