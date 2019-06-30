package org.moera.node.util;

import javax.servlet.http.HttpServletResponse;

import org.springframework.util.StringUtils;

public class VirtualPageHeader {

    public static final String X_MOERA = "X-Moera";

    public static String build(String page) {
        return "page=" + Util.ue(page);
    }

    public static void put(HttpServletResponse response, String page) {
        if (!StringUtils.isEmpty(page)) {
            response.addHeader(X_MOERA, build(page));
        } else {
            response.addHeader(X_MOERA, "true");
        }
    }

}
