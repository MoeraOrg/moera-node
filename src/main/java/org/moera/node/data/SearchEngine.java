package org.moera.node.data;

import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.moera.node.global.UserAgent;

public enum SearchEngine {

    GOOGLE("^https?://[a-z]+\\.google\\.com", UserAgent.GOOGLEBOT);

    private final Pattern refererPattern;
    private final UserAgent botUserAgent;

    SearchEngine(String refererRegex, UserAgent botUserAgent) {
        this.refererPattern = Pattern.compile(refererRegex);
        this.botUserAgent = botUserAgent;
    }

    public Pattern getRefererPattern() {
        return refererPattern;
    }

    public UserAgent getBotUserAgent() {
        return botUserAgent;
    }

    @JsonValue
    public String getValue() {
        return name().toLowerCase().replace('_', '-');
    }

    public static String toValue(SearchEngine type) {
        return type != null ? type.getValue() : null;
    }

    public static SearchEngine forValue(String value) {
        try {
            return parse(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @JsonCreator
    public static SearchEngine parse(String value) {
        return valueOf(value.toUpperCase().replace('-', '_'));
    }

}
