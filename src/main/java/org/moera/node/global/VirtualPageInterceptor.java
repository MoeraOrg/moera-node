package org.moera.node.global;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.util.VirtualPageHeader;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class VirtualPageInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        if (requestContext.isRegistrar() || requestContext.isUndefinedDomain()) {
            return true;
        }
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        VirtualPage virtualPage = ((HandlerMethod) handler).getMethodAnnotation(VirtualPage.class);
        if (virtualPage == null) {
            return true;
        }
        VirtualPageHeader.put(response, requestContext.nodeName(), virtualPage.value());

        if (request.getHeader("X-Accept-Moera") != null) {
            // No redirect and no content, because a Moera client is making this request
            return false;
        }
        if (requestContext.isAutoClient()) {
            response.sendRedirect(requestContext.getRedirectorUrl());
            return false;
        }

        return true;
    }

}
