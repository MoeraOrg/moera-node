package org.moera.node.text.sanitizer;

import java.util.List;
import java.util.Set;

import org.moera.lib.node.types.body.Body;
import org.moera.node.data.MediaFileOwner;
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
                    "ins", "details", "summary", "mr-spoiler", "mr-spoiler-block", "iframe", "video", "audio")
            .allowUrlProtocols("http", "https", "ftp", "mailto")
            .allowAttributes("style").onElements("p")
            .allowAttributes("start", "type").onElements("ol")
            .allowAttributes("href", "data-nodename", "data-id").onElements("a")
            .allowAttributes("src", "srcset", "sizes", "width", "height", "alt", "style").onElements("img")
            .allowAttributes("title").onElements("mr-spoiler", "mr-spoiler-block")
            .allowAttributes("src").matching(HtmlSanitizer::validateIframeSrc).onElements("iframe")
            .allowAttributes("width", "height", "frameborder", "allow", "allowfullscreen", "sandbox", "scrolling",
                    "allowtransparency", "style").onElements("iframe")
            .allowAttributes("dir").globally()
            // .allowStyling(CssSchema.withProperties(Set.of("text-align", "width", "height", "--width", "--height")))
            // TODO CSS schema does not support CSS variables
            .withPreprocessor(ParagraphProcessor::new)
            .toFactory();
    private static final Set<String> IFRAME_HOSTNAMES = Set.of(
            "c.simmer.io", "codepen.io", "docs.google.com", "itch.io", "odysee.com", "open.spotify.com", "peer.tube",
            "player.vimeo.com", "rumble.com", "www.facebook.com", "www.youtube-nocookie.com", "www.youtube.com",
            "gifer.com", "giphy.com"
    );
    private static final PolicyFactory SAFE_HTML = BASIC_HTML
            .and(new HtmlPolicyBuilder()
                    .allowElements("h1", "h2", "h3", "h4", "h5", "h6")
                    .allowAttributes("target")
                        .matching(false, "_blank")
                        .onElements("a")
                    .allowAttributes("class")
                        .matching(new ClassPolicy(false))
                        .globally()
                    .toFactory());
    private static final PolicyFactory SAFE_PREVIEW_HTML = BASIC_HTML
            .and(new HtmlPolicyBuilder()
                    .allowElements(
                            (elementName, attrs) -> "b",
                            "h1", "h2", "h3", "h4", "h5", "h6")
                    .allowAttributes("class")
                        .matching(new ClassPolicy(true))
                        .globally()
                    .toFactory());

    private static boolean validateIframeSrc(String src) {
        String hostname = UriComponentsBuilder.fromHttpUrl(src).build().getHost();
        return IFRAME_HOSTNAMES.contains(hostname);
    }

    private static String sanitize(String html, boolean preview, List<MediaFileOwner> media) {
        if (html == null) {
            return null;
        }
        PolicyFactory policyFactory = preview ? SAFE_PREVIEW_HTML : SAFE_HTML;
        policyFactory = policyFactory.and(new HtmlPolicyBuilder()
                .withPreprocessor(u -> new ImageProcessor(u, media))
                .toFactory());
        return policyFactory.sanitize(html);
    }

    private static String sanitizeIfNeeded(String html, boolean preview, List<MediaFileOwner> media) {
        String saneHtml = sanitize(html, preview, media);
        return saneHtml == null || saneHtml.equals(html) ? null : saneHtml;
    }

    public static String sanitizeIfNeeded(Body body, boolean preview, List<MediaFileOwner> media) {
        return sanitizeIfNeeded(body.getText(), preview, media);
    }

}
