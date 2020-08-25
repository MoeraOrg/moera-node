package org.moera.node.test;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.internal.hamcrest.HamcrestArgumentMatcher;

public class HtmlMatcher extends BaseMatcher<String> {
    private final Element expected;

    public HtmlMatcher(String expected) {
        this.expected = normalizeHtml(expected);
    }

    private static Element normalizeHtml(String html) {
        Element root = new Element("div");
        return root.insertChildren(0, Parser.parseFragment(html, root, ""));
    }

    @Override
    public boolean matches(Object o) {
        if (!(o instanceof String)) return false;
        return expected.hasSameValue(normalizeHtml((String) o));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(expected.html());
    }

    @Override
    public void describeMismatch(Object item, Description description) {
        if (item instanceof String) {
            description.appendText("was ").appendText(normalizeHtml((String) item).html());
        } else {
            description.appendValue(item).appendText(" is not String");
        }
    }

    public static ArgumentMatcher<String> eqHtml(String expected) {
        return new HamcrestArgumentMatcher<>(new HtmlMatcher(expected));
    }

    public static void assertEqHtml(String expected, String actual) {
        Assert.assertThat(actual, new HtmlMatcher(expected));
    }
}
