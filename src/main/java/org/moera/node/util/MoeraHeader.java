package org.moera.node.util;

public class MoeraHeader {

    public static final String X_MOERA = "X-Moera";

    public static String build(String page) {
        return "page=" + Util.ue(page);
    }

}
