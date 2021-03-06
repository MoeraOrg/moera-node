package org.moera.node.auth;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Objects;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.config.Config;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UserAgent;
import org.moera.node.global.UserAgentOs;
import org.moera.node.model.Result;
import org.moera.node.util.UriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class AuthenticationInterceptor extends HandlerInterceptorAdapter {

    private static class Secrets {
        public String rootSecret;
        public String token;
        public String carte;
    }

    private static Logger log = LoggerFactory.getLogger(AuthenticationInterceptor.class);

    @Inject
    private Config config;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private RequestContext requestContext;

    @Inject
    private MessageSource messageSource;

    @Inject
    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() throws RootSecretNotSetException {
        if (StringUtils.isEmpty(config.getRootSecret())) {
            throw new RootSecretNotSetException();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        try {
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
        } catch (InvalidTokenException e) {
            handleError(response, HttpStatus.UNAUTHORIZED, "authentication.invalid",
                    "Bearer realm=\"Node\" error=\"invalid_token\"");
            return false;
        } catch (AuthenticationException e) {
            handleError(response, HttpStatus.FORBIDDEN, "authentication.required",
                    "Bearer realm=\"Node\"");
            return false;
        }
    }

    private void handleError(HttpServletResponse response, HttpStatus status, String errorCode, String wwwAuthHeader)
            throws IOException {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthHeader);
        response.setStatus(status.value());
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        objectMapper.writeValue(response.getWriter(), new Result(errorCode, message));
    }

    private Secrets extractSecrets(HttpServletRequest request) {
        Secrets secrets = new Secrets();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.isEmpty(authHeader)) {
            String[] parts = StringUtils.split(authHeader, " ");
            if (parts != null && parts[0].trim().equalsIgnoreCase("bearer")) {
                String auth = parts[1].trim();
                if (auth.startsWith("secret:")) {
                    secrets.rootSecret = auth.substring(7);
                } else if (auth.startsWith("token:")) {
                    secrets.token = auth.substring(6);
                } else if (auth.startsWith("carte:")) {
                    secrets.carte = auth.substring(6);
                } else {
                    secrets.token = auth;
                }
                return secrets;
            }
        }
        return secrets;
    }

    private void processAuthParameters(HttpServletRequest request) throws InvalidTokenException, UnknownHostException {
        requestContext.setLocalAddr(InetAddress.getByName(request.getLocalAddr()));
        requestContext.setBrowserExtension(request.getHeader("X-Accept-Moera") != null);
        Secrets secrets = extractSecrets(request);
        if (Objects.equals(config.getRootSecret(), secrets.rootSecret)) {
            requestContext.setRootAdmin(true);
            MDC.put("auth", "!");
        }
        requestContext.setAdmin(authenticationManager.isAdminToken(secrets.token, requestContext.nodeId()));
        MDC.put("auth", requestContext.isAdmin() ? "#" : "$");
        try {
            requestContext.setClientName(
                    authenticationManager.getClientName(secrets.carte, UriUtil.remoteAddress(request)));
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
