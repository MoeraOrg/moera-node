package org.moera.node.text;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.moera.lib.node.types.body.Body;
import org.moera.lib.node.types.body.LinkPreview;
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

    public static Set<String> extractMediaFileIds(List<LinkPreview> previews) {
        if (previews == null || previews.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> ids = new HashSet<>();
        previews.stream()
            .map(LinkPreview::getImageHash)
            .filter(h -> !ObjectUtils.isEmpty(h))
            .forEach(ids::add);
        return ids;
    }

    public static Set<String> extractMediaFileIds(Body body) {
        Set<String> ids = extractMediaFileIds(body.getText());
        ids.addAll(extractMediaFileIds(body.getLinkPreviews()));
        body.getLinkPreviews().stream()
            .map(LinkPreview::getImageHash)
            .filter(h -> !ObjectUtils.isEmpty(h))
            .forEach(ids::add);
        return ids;
    }

}
