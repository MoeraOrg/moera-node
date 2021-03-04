package org.moera.node.text.shorten;

import java.util.Set;

import org.jsoup.nodes.Element;

class Elements {

    private static final Set<String> OBJECTS = Set.of("img", "iframe", "video");
    private static final int OBJECT_DEFAULT_HEIGHT = 600;

    public static boolean isBody(Element element) {
        return element.normalName().equals("body");
    }

    public static boolean isDetails(Element element) {
        return element.normalName().equals("details");
    }

    public static boolean isBreaking(Element element) {
        return element.normalName().equals("br") || element.tag().isBlock();
    }

    public static boolean isObject(Element element) {
        return OBJECTS.contains(element.normalName());
    }

    public static int getHeight(Element element) {
        try {
            return Integer.parseInt(element.attr("height"));
        } catch (Exception e) {
            return OBJECT_DEFAULT_HEIGHT;
        }
    }

}
