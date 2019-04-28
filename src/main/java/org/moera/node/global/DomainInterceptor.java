package org.moera.node.global;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.option.Domains;
import org.moera.node.util.UriUtil;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class DomainInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private Domains domains;

    @Inject
    private RequestContext requestContext;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        requestContext.setOptions(domains.getDomainOptions(UriUtil.createBuilderFromRequest(request).build().getHost()));
        return true;
    }

}
