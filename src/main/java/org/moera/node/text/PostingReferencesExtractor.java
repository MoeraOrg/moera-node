package org.moera.node.text;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.NodeFilter;
import org.moera.lib.Rules;
import org.moera.lib.UniversalLocation;
import org.moera.lib.naming.NodeName;
import org.moera.lib.naming.NodeNameParsingException;
import org.moera.lib.node.types.body.Body;

public class PostingReferencesExtractor {

    public record Reference(String nodeName, String postingId) {
    }

    private static final Set<String> ALLOWED_DOMAINS = Set.of("moera.page", "web.moera.org");

    private static final Pattern POSTING_PATH = Pattern.compile("/post/([^/]+)");

    public static Set<Reference> extract(Body body) {
        if (body.getText() == null) {
            return Collections.emptySet();
        }
        return extract(body.getText());
    }

    private static Set<Reference> extract(String html) {
        Document document = Jsoup.parseBodyFragment(html);
        Extractor extractor = new Extractor();
        document.filter(extractor);
        return extractor.getResult();
    }

    private static boolean hasOtherTarget(String query) {
        if (query == null) {
            return false;
        }
        for (String parameter : query.split("&")) {
            String name = URLDecoder.decode(parameter.split("=", 2)[0], StandardCharsets.UTF_8);
            if (name.equals("comment") || name.equals("media")) {
                return true;
            }
        }
        return false;
    }

    private static final class Extractor implements NodeFilter {

        private final Set<Reference> result = new HashSet<>();

        Set<Reference> getResult() {
            return result;
        }

        @Override
        public FilterResult head(Node node, int depth) {
            if (node instanceof Element element && element.normalName().equals("a")) {
                extractReference(element.attr("href"));
            }
            return FilterResult.CONTINUE;
        }

        @Override
        public FilterResult tail(Node node, int depth) {
            return FilterResult.CONTINUE;
        }

        private void extractReference(String url) {
            try {
                URI uri = new URI(url);
                if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                    return;
                }
                String host = uri.getHost();
                if (host == null || !ALLOWED_DOMAINS.contains(host.toLowerCase(Locale.ROOT))) {
                    return;
                }
                if (hasOtherTarget(uri.getRawQuery())) {
                    return;
                }
                UniversalLocation location = new UniversalLocation(url);
                if (location.getNodeName() == null || location.getPath() == null) {
                    return;
                }
                Matcher posting = POSTING_PATH.matcher(location.getPath());
                NodeName nodeName = NodeName.parse(location.getNodeName());
                if (posting.matches() && Rules.isNameValid(nodeName.getName()) && nodeName.getGeneration() >= 0) {
                    result.add(new Reference(nodeName.toString(), posting.group(1)));
                }
            } catch (URISyntaxException | NodeNameParsingException | IndexOutOfBoundsException ignored) {
                // A malformed link must not prevent scanning the rest of the posting.
            }
        }

    }

}
