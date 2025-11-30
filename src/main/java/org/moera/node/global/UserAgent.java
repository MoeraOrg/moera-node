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
    SEMRUSHBOT("SemrushBot"),
    IE("IE"),
    EDGE("Edge"),
    SAFARI("Safari"),
    BINGBOT("bingbot"),
    YANDEXBOT("YandexBot");

    private final String title;

    UserAgent(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

}
