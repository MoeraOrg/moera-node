package org.moera.node.global;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.commons.util.Util;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class PermissionsInterceptor extends HandlerInterceptorAdapter {

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
        String tokenS = request.getParameter("token");
        if (!StringUtils.isEmpty(tokenS)) {
            Token token = tokenRepository.findById(tokenS).orElse(null);
            if (token == null || token.getDeadline().before(Util.now())) {
                throw new InvalidTokenException();
            }
            requestContext.setAdmin(token.isAdmin());
        }
    }

}
