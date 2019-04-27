package org.moera.node.global;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.commons.util.Util;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class PermissionsInterceptor extends HandlerInterceptorAdapter {

    private static Logger log = LoggerFactory.getLogger(PermissionsInterceptor.class);

    @Inject
    private Options options;

    @Inject
    private TokenRepository tokenRepository;

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        processAuthParameters(request);

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        if (((HandlerMethod) handler).getMethodAnnotation(Admin.class) == null) {
            return true;
        }
        if (!requestContext.isAdmin()) {
            throw new AuthorizationException();
        }

        return true;
    }

    private void processAuthParameters(HttpServletRequest request) throws InvalidTokenException {
        requestContext.setBrowserExtension(request.getHeader("X-Accept-Moera") != null);
        String tokenS = request.getParameter("token");
        if (!StringUtils.isEmpty(tokenS)) {
            Token token = tokenRepository.findById(tokenS).orElse(null);
            if (token == null
                    || !token.getNodeId().equals(options.nodeId())
                    || token.getDeadline().before(Util.now())) {
                throw new InvalidTokenException();
            }
            requestContext.setAdmin(token.isAdmin());
            log.info("Authorized as {}", token.isAdmin() ? "admin" : "non-admin");
        }
    }

}
