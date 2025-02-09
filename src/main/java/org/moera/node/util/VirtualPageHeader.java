package org.moera.node.util;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.util.ObjectUtils;

public class VirtualPageHeader {

    public static final String X_MOERA = "X-Moera";

    public static String build(String nodeName, String page) {
        StringBuilder buf = new StringBuilder();
        if (!ObjectUtils.isEmpty(nodeName)) {
            buf.append("name=");
            buf.append(Util.ue(nodeName));
        }
        if (!ObjectUtils.isEmpty(page)) {
            if (!buf.isEmpty()) {
                buf.append(' ');
            }
            buf.append("page=");
            buf.append(page);
        }
        return buf.toString();
    }

    public static void put(HttpServletResponse response, String nodeName, String page) {
        String header = build(nodeName, page);
        if (!ObjectUtils.isEmpty(header)) {
            response.addHeader(X_MOERA, header);
        } else {
            response.addHeader(X_MOERA, "true");
        }
    }

}
