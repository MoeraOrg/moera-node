package org.moera.node.global;

public enum UserAgent {

    UNKNOWN("unknown"),
    FIREFOX("Firefox"),
    CHROME("Chrome"),
    OPERA("Opera"),
    YANDEX("Yandex"),
    BRAVE("Brave"),
    VIVALDI("Vivaldi"),
    DOLPHIN("Dolphin"),
    GOOGLEBOT("Googlebot"),
    PETALBOT("PetalBot"),
    IE("IE"),
    EDGE("Edge"),
    SAFARI("Safari");

    private final String title;

    UserAgent(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
