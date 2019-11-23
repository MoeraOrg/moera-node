package org.moera.node.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class UriUtil {

    public static UriComponentsBuilder createBuilderFromRequest(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(request.getRequestURL().toString())
                .query(request.getQueryString());
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (!StringUtils.isEmpty(forwardedHost)) {
            builder.host(forwardedHost);
            String forwardedPort = request.getHeader("X-Forwarded-Port");
            builder.port(forwardedPort);
            String forwardedScheme = request.getHeader("X-Forwarded-Proto");
            if (!StringUtils.isEmpty(forwardedScheme)) {
                builder.scheme(forwardedScheme);
            }
        }
        UriComponents components = builder.build();
        if (components.getScheme() != null
                && (components.getScheme().equalsIgnoreCase("https") && components.getPort() == 443
                    || components.getScheme().equalsIgnoreCase("http") && components.getPort() == 80)) {
            builder.port(null);
        }
        return builder;
    }

    public static UriComponentsBuilder createLocalBuilderFromRequest(HttpServletRequest request) {
        return UriComponentsBuilder
                .fromPath(request.getRequestURI())
                .query(request.getQueryString());
    }

    public static String normalize(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

}
