package org.moera.node.auth;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.global.UserAgent;
import org.moera.node.global.UserAgentOs;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class UserAgentInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userAgent = request.getHeader("User-Agent");
        if (ObjectUtils.isEmpty(userAgent)) {
            return true;
        }

        if (userAgent.contains("Firefox")) {
            requestContext.setUserAgent(UserAgent.FIREFOX);
        } else if (userAgent.contains("Opera")) {
            requestContext.setUserAgent(UserAgent.OPERA);
        } else if (userAgent.contains("Googlebot")) {
            requestContext.setUserAgent(UserAgent.GOOGLEBOT);
        } else if (userAgent.contains("bingbot")) {
            requestContext.setUserAgent(UserAgent.BINGBOT);
        } else if (userAgent.contains("yandex.com/bots")) {
            requestContext.setUserAgent(UserAgent.YANDEXBOT);
        } else if (userAgent.contains("PetalBot")) {
            requestContext.setUserAgent(UserAgent.PETALBOT);
        } else if (userAgent.contains("SemrushBot")) {
            requestContext.setUserAgent(UserAgent.SEMRUSHBOT);
        } else if (userAgent.contains("MJ12bot")) {
            requestContext.setUserAgent(UserAgent.MJ12BOT);
        } else if (userAgent.contains("Amazonbot")) {
            requestContext.setUserAgent(UserAgent.AMAZONBOT);
        } else if (userAgent.contains("OAI-SearchBot")) {
            requestContext.setUserAgent(UserAgent.OAISEARCHBOT);
        } else if (userAgent.contains("Applebot")) {
            requestContext.setUserAgent(UserAgent.APPLEBOT);
        } else if (userAgent.contains("Chrome")) {
            if (userAgent.contains("YaBrowser")) {
                requestContext.setUserAgent(UserAgent.YANDEX);
            } else if (userAgent.contains("Brave")) {
                requestContext.setUserAgent(UserAgent.BRAVE);
            } else if (userAgent.contains("Vivaldi")) {
                requestContext.setUserAgent(UserAgent.VIVALDI);
            } else if (userAgent.contains("Edge")) {
                requestContext.setUserAgent(UserAgent.EDGE);
            } else {
                requestContext.setUserAgent(UserAgent.CHROME);
            }
        } else if (userAgent.contains("Safari")) {
            requestContext.setUserAgent(UserAgent.SAFARI);
        } else if (userAgent.contains("MSIE")) {
            requestContext.setUserAgent(UserAgent.IE);
        } else if (userAgent.contains("Dolphin")) {
            requestContext.setUserAgent(UserAgent.DOLPHIN);
        }

        if (userAgent.contains("Android")) {
            requestContext.setUserAgentOs(UserAgentOs.ANDROID);
        } else if (userAgent.contains("iPhone")) {
            requestContext.setUserAgentOs(UserAgentOs.IOS);
        } else if (userAgent.contains("Windows")) {
            requestContext.setUserAgentOs(UserAgentOs.WINDOWS);
        } else if (userAgent.contains("Linux")) {
            requestContext.setUserAgentOs(UserAgentOs.LINUX);
        }

        return true;
    }

}
