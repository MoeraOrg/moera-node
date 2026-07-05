package org.moera.node.global;

public enum UserAgent {

    UNKNOWN("unknown", true),
    FIREFOX("Firefox", false),
    CHROME("Chrome", false),
    OPERA_PRESTO("Opera-Presto", true), // not compatible with modern web applications
    OPERA("Opera", false),
    YANDEX("Yandex", false),
    BRAVE("Brave", false),
    VIVALDI("Vivaldi", false),
    DOLPHIN("Dolphin", false),
    GOOGLEBOT("Googlebot", true),
    PETALBOT("PetalBot", true),
    SEMRUSHBOT("SemrushBot", true),
    MJ12BOT("MJ12bot", true),
    AMAZONBOT("Amazonbot", true),
    OAISEARCHBOT("OAISearchBot", true),
    APPLEBOT("Applebot", true),
    IE("IE", true), // not compatible with modern web applications
    EDGE("Edge", false),
    SAFARI("Safari", false),
    BINGBOT("bingbot", true),
    YANDEXBOT("YandexBot", true);

    private final String title;
    private final boolean bot;

    UserAgent(String title, boolean bot) {
        this.title = title;
        this.bot = bot;
    }

    public String getTitle() {
        return title;
    }

    public boolean isBot() {
        return bot;
    }

}
