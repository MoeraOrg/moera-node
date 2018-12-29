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
public class AdminInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private TokenRepository tokenRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        if (((HandlerMethod) handler).getMethodAnnotation(Admin.class) == null) {
            return true;
        }
        String tokenS = request.getParameter("token");
        if (StringUtils.isEmpty(tokenS)) {
            throw new AuthorizationException();
        }
        Token token = tokenRepository.findById(tokenS).orElse(null);
        if (token == null || token.getDeadline().before(Util.now()) || !token.isAdmin()) {
            throw new InvalidTokenException();
        }
        return true;
    }

}
