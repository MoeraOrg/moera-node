package org.moera.node.util;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

public class UriUtil {

    public static UriComponentsBuilder createBuilderFromRequest(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(request.getRequestURL().toString())
                .query(request.getQueryString());
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (!StringUtils.isEmpty(forwardedHost)) {
            builder.host(forwardedHost);
        }
        String forwardedPort = request.getHeader("X-Forwarded-Port");
        if (!StringUtils.isEmpty(forwardedPort)) {
            builder.port(forwardedPort);
        }
        return builder;
    }

    public static UriComponentsBuilder createLocalBuilderFromRequest(HttpServletRequest request) {
        return UriComponentsBuilder
                .fromPath(request.getRequestURI())
                .query(request.getQueryString());
    }

}
