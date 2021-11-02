package org.moera.node.text;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaExtractor {

    private static final Pattern HASH_URI_PATTERN = Pattern.compile("[\"']hash:([A-Za-z0-9/-]+={0,2})[\"']");

    public static Set<String> extractMediaFileIds(String html) {
        Set<String> ids = new HashSet<>();
        Matcher matcher = HASH_URI_PATTERN.matcher(html);
        while (matcher.find()) {
            ids.add(matcher.group(1));
        }
        return ids;
    }

}
