package org.moera.node.global;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.model.OperationFailure;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class EntitledInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod) || !((HandlerMethod) handler).hasMethodAnnotation(Entitled.class)) {
            return true;
        }
        if (ObjectUtils.isEmpty(requestContext.nodeName())) {
            throw new OperationFailure("node-name-not-set");
        }
        if (requestContext.getOptions().getPrivateKey("profile.signing-key") == null) {
            throw new OperationFailure("signing-key-not-set");
        }
        return true;
    }

}
