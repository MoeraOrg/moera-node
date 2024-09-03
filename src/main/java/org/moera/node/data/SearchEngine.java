package org.moera.node.data;

import java.util.regex.Pattern;

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

}
