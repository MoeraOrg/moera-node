package org.moera.node.text;

import java.util.Set;

import org.moera.node.model.Body;
import org.owasp.html.CssSchema;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.web.util.UriComponentsBuilder;

public class HtmlSanitizer {

    private static final PolicyFactory BASIC_HTML = new HtmlPolicyBuilder()
            .allowElements("address", "aside", "footer", "header", "hgroup", "nav", "section", "blockquote", "dd",
                    "div", "dl", "dt", "figcaption", "figure", "hr", "li", "ol", "p", "pre", "ul", "a", "abbr", "b",
                    "bdi", "bdo", "br", "cite", "code", "data", "dfn", "em", "i", "kbd", "mark", "q", "rb", "rp", "rt",
                    "rtc", "ruby", "s", "samp", "small", "span", "strong", "sub", "sup", "time", "u", "var", "wbr",
                    "caption", "col", "colgroup", "table", "tbody", "td", "tfoot", "th", "thead", "tr", "img", "del",
                    "ins", "details", "summary", "mr-spoiler", "iframe")
            .allowUrlProtocols("http", "https", "ftp", "mailto")
            .allowAttributes("style").onElements("p")
            .allowAttributes("href", "data-nodename").onElements("a")
            .allowAttributes("src", "srcset", "width", "height", "alt").onElements("img")
            .allowAttributes("class")
                .matching(false, "emoji")
                .onElements("img")
            .allowAttributes("title").onElements("mr-spoiler")
            .allowAttributes("src").matching(HtmlSanitizer::validateIframeSrc).onElements("iframe")
            .allowAttributes("width", "height", "frameborder", "allow", "allowfullscreen", "sandbox", "scrolling",
                    "allowtransparency").onElements("iframe")
            .allowAttributes("dir").globally()
            .allowStyling(CssSchema.withProperties(Set.of("text-align")))
            .toFactory();
    private static final Set<String> IFRAME_HOSTNAMES = Set.of(
            "www.youtube.com", "player.vimeo.com", "www.facebook.com", "peer.tube"
    );
    private static final PolicyFactory SAFE_HTML = BASIC_HTML
            .and(new HtmlPolicyBuilder()
                    .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                    .allowAttributes("target")
                        .matching(false, "_blank")
                        .onElements("a")
                    .toFactory());
    private static final PolicyFactory SAFE_PREVIEW_HTML = BASIC_HTML
            .and(new HtmlPolicyBuilder()
                    .allowElements(
                            (elementName, attrs) -> "b",
                            "h1", "h2", "h3", "h4", "h5", "h6")
                    .toFactory());

    private static boolean validateIframeSrc(String src) {
        String hostname = UriComponentsBuilder.fromHttpUrl(src).build().getHost();
        return IFRAME_HOSTNAMES.contains(hostname);
    }

    private static String sanitize(String html, boolean preview) {
        if (html == null) {
            return null;
        }
        return (preview ? SAFE_PREVIEW_HTML : SAFE_HTML).sanitize(html);
    }

    private static String sanitizeIfNeeded(String html, boolean preview) {
        String saneHtml = sanitize(html, preview);
        return saneHtml == null || saneHtml.equals(html) ? null : saneHtml;
    }

    public static String sanitizeIfNeeded(Body body, boolean preview) {
        return sanitizeIfNeeded(body.getText(), preview);
    }

}
