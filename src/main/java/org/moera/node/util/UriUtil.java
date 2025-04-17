package org.moera.node.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.ObjectUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class UriUtil {

    public static UriComponentsBuilder createBuilderFromRequest(HttpServletRequest request) {
        UriComponentsBuilder builder = UriComponentsBuilder
            .fromUriString(request.getRequestURL().toString())
            .query(request.getQueryString());
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (!ObjectUtils.isEmpty(forwardedHost)) {
            builder.host(forwardedHost);
            String forwardedPort = request.getHeader("X-Forwarded-Port");
            builder.port(forwardedPort);
            String forwardedScheme = request.getHeader("X-Forwarded-Proto");
            if (!ObjectUtils.isEmpty(forwardedScheme)) {
                builder.scheme(forwardedScheme);
            }
        }
        UriComponents components = builder.build();
        if (components.getScheme() != null
            && (
                components.getScheme().equalsIgnoreCase("https") && components.getPort() == 443
                || components.getScheme().equalsIgnoreCase("http") && components.getPort() == 80
            )
        ) {
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

    public static InetAddress remoteAddress(HttpServletRequest request) throws UnknownHostException {
        String forwardedAddress = request.getHeader("X-Forwarded-For");
        return InetAddress.getByName(
            !ObjectUtils.isEmpty(forwardedAddress)
                ? forwardedAddress
                : request.getRemoteAddr()
        );
    }

    public static InetAddress remoteAddress(ServerHttpRequest request) throws UnknownHostException {
        String forwardedAddress = request.getHeaders().getFirst("X-Forwarded-For");
        return InetAddress.getByName(
            !ObjectUtils.isEmpty(forwardedAddress)
                ? forwardedAddress
                : request.getRemoteAddress().getAddress().getHostAddress()
        );
    }

    public static String siteUrl(String host, int port) {
        return switch (port) {
            case 80 -> "http://%s".formatted(host);
            case 443 -> "https://%s".formatted(host);
            default -> port > 0 ? "http://%s:%d".formatted(host, port) : "https://%s".formatted(host);
        };
    }

}
