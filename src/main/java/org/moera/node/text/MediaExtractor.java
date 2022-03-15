package org.moera.node.text;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.moera.node.model.body.Body;
import org.moera.node.model.body.LinkPreview;
import org.springframework.util.ObjectUtils;

public class MediaExtractor {

    private static final Pattern HASH_URI_PATTERN = Pattern.compile("[\"']hash:([A-Za-z0-9_-]+={0,2})[\"']");

    private static Set<String> extractMediaFileIds(String html) {
        Set<String> ids = new HashSet<>();
        Matcher matcher = HASH_URI_PATTERN.matcher(html);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

    public static Set<String> extractMediaFileIds(Body body) {
        Set<String> ids = extractMediaFileIds(body.getText());
        body.getLinkPreviews().stream()
                .map(LinkPreview::getImageHash)
                .filter(h -> !ObjectUtils.isEmpty(h))
                .forEach(ids::add);
        return ids;
    }

}
