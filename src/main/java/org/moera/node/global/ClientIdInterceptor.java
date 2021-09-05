package org.moera.node.global;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class ClientIdInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String cid = request.getParameter("cid");
        requestContext.setClientId(ObjectUtils.isEmpty(cid) ? null : cid);

        return true;
    }

}
