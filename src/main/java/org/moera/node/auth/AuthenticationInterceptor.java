package org.moera.node.auth;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.config.Config;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UserAgent;
import org.moera.node.global.UserAgentOs;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class AuthenticationInterceptor extends HandlerInterceptorAdapter {

    private static Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    @Inject
    private Config config;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private RequestContext requestContext;

    @PostConstruct
    public void init() throws RootSecretNotSetException {
        if (StringUtils.isEmpty(config.getRootSecret())) {
            throw new RootSecretNotSetException();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        processAuthParameters(request);
        processUserAgent(request);

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        if (((HandlerMethod) handler).hasMethodAnnotation(RootAdmin.class) && !requestContext.isRootAdmin()) {
            throw new AuthenticationException();
        }
        if ((((HandlerMethod) handler).hasMethodAnnotation(Admin.class)
                || ((HandlerMethod) handler).getBeanType().isAnnotationPresent(Admin.class))
                && !requestContext.isAdmin()) {
            throw new AuthenticationException();
        }

        return true;
    }

    private void processAuthParameters(HttpServletRequest request) throws InvalidTokenException, UnknownHostException {
        requestContext.setLocalAddr(InetAddress.getByName(request.getLocalAddr()));
        requestContext.setBrowserExtension(request.getHeader("X-Accept-Moera") != null);
        String secret = request.getParameter("secret");
        if (Objects.equals(config.getRootSecret(), secret)) {
            requestContext.setRootAdmin(true);
            log.info("Authorized as root admin");
        }
        requestContext.setAdmin(
                authenticationManager.isAdminToken(request.getParameter("token"), requestContext.nodeId()));
        log.info("Authorized as {}", requestContext.isAdmin() ? "admin" : "non-admin");
        try {
            requestContext.setClientName(
                    authenticationManager.getClientName(request.getParameter("carte"), UriUtil.remoteAddress(request)));
        } catch (UnknownHostException e) {
            throw new InvalidCarteException("carte.client-address-unknown");
        }
        if (!requestContext.isAdmin() && !StringUtils.isEmpty(requestContext.getClientName())) {
            log.info("Authorized with node name {}", requestContext.getClientName());
        }
    }

    private void processUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (StringUtils.isEmpty(userAgent)) {
            return;
        }

        if (userAgent.contains("Firefox")) {
            requestContext.setUserAgent(UserAgent.FIREFOX);
        } else if (userAgent.contains("Opera")) {
            requestContext.setUserAgent(UserAgent.OPERA);
        } else if (userAgent.contains("Chrome")) {
            if (userAgent.contains("YaBrowser")) {
                requestContext.setUserAgent(UserAgent.YANDEX);
            } else if (userAgent.contains("Brave")) {
                requestContext.setUserAgent(UserAgent.BRAVE);
            } else if (userAgent.contains("Vivaldi")) {
                requestContext.setUserAgent(UserAgent.VIVALDI);
            } else {
                requestContext.setUserAgent(UserAgent.CHROME);
            }
        } else if (userAgent.contains("Dolphin")) {
            requestContext.setUserAgent(UserAgent.DOLPHIN);
        }

        if (userAgent.contains("Android")) {
            requestContext.setUserAgentOs(UserAgentOs.ANDROID);
        } else if (userAgent.contains("iPhone")) {
            requestContext.setUserAgentOs(UserAgentOs.IOS);
        }
    }

}
