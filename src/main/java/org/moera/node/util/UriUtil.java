package org.moera.node.util;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
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

    public static String normalize(String uri) {
        if (uri == null) {
            return null;
        }
        return uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    public static String resolve(String uri, String baseUri) {
        if (uri == null) {
            return null;
        }
        URI parsed = URI.create(uri);
        return parsed.isAbsolute() ? uri : URI.create(baseUri).resolve(parsed).toString();
    }

    public static String fileName(String uri) {
        if (uri == null) {
            return null;
        }
        String path = UriComponentsBuilder.fromUriString(uri).build().getPath();
        if (path == null) {
            return null;
        }
        int end = path.length() - 1;
        while (end >= 0 && path.charAt(end) == '/') {
            end--;
        }
        if (end < 0) {
            return "";
        }
        int pos = path.lastIndexOf('/', end);
        return pos >= 0 ? path.substring(pos + 1, end + 1) : path.substring(0, end + 1);
    }

    public static String stripQueryAndFragment(String uri) {
        if (uri == null) {
            return null;
        }
        int suffixStart = uri.indexOf('?');
        if (suffixStart < 0) {
            suffixStart = uri.indexOf('#');
        }
        return suffixStart >= 0 ? uri.substring(0, suffixStart) : uri;
    }

    public static String query(String uri) {
        return uri != null ? URI.create(uri).getRawQuery() : null;
    }

    public static String queryParameter(String query, String name) {
        String value = encodedQueryParameter(query, name);
        return value != null ? URLDecoder.decode(value, StandardCharsets.UTF_8) : null;
    }

    public static String encodedQueryParameter(String query, String name) {
        String encodedName = Util.ue(name);
        for (String parameter : query.split("&")) {
            int equals = parameter.indexOf('=');
            if (equals >= 0 && parameter.substring(0, equals).equals(encodedName)) {
                return parameter.substring(equals + 1);
            }
        }
        return null;
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
