package org.moera.node.auth;

import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class PermissionsInterceptor extends HandlerInterceptorAdapter {

    private static Logger log = LoggerFactory.getLogger(PermissionsInterceptor.class);

    @Value("${node.root-secret}")
    private String rootSecret;

    @Inject
    private AuthenticationManager authenticationManager;

    @Inject
    private RequestContext requestContext;

    @PostConstruct
    public void init() throws RootSecretNotSetException {
        if (StringUtils.isEmpty(rootSecret)) {
            throw new RootSecretNotSetException();
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        processAuthParameters(request);

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

    private void processAuthParameters(HttpServletRequest request) throws InvalidTokenException {
        requestContext.setBrowserExtension(request.getHeader("X-Accept-Moera") != null);
        String secret = request.getParameter("secret");
        if (rootSecret.equals(secret)) {
            requestContext.setRootAdmin(true);
            log.info("Authorized as root admin");
        }
        requestContext.setAdmin(
                authenticationManager.isAdminToken(request.getParameter("token"), requestContext.nodeId()));
        log.info("Authorized as {}", requestContext.isAdmin() ? "admin" : "non-admin");
        try {
            requestContext.setClientName(authenticationManager.getClientName(request.getParameter("carte"),
                    InetAddress.getByName(request.getRemoteAddr())));
        } catch (UnknownHostException e) {
            throw new InvalidCarteException("carte.client-address-unknown");
        }
        if (!requestContext.isAdmin() && !StringUtils.isEmpty(requestContext.getClientName())) {
            log.info("Authorized with node name {}", requestContext.getClientName());
        }
    }

}
