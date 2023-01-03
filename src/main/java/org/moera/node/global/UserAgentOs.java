package org.moera.node.global;

public enum UserAgentOs {

    UNKNOWN("unknown"),
    ANDROID("Android"),
    IOS("iOS"),
    WINDOWS("Windows"),
    LINUX("Linux");

    private final String title;

    UserAgentOs(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
