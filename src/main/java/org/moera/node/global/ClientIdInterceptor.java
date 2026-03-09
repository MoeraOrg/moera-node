package org.moera.node.global;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ClientIdInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String cid = request.getParameter("cid");
        if (ObjectUtils.isEmpty(cid)) {
            cid = request.getHeader("Client-ID");
        }
        requestContext.setClientId(ObjectUtils.isEmpty(cid) ? null : cid);

        return true;
    }

}
