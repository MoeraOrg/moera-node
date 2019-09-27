package org.moera.node.domain;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.util.UriUtil;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.UriComponents;

@Component
public class DomainInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private Domains domains;

    @Inject
    private RequestContext requestContext;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        UriComponents uriComponents = UriUtil.createBuilderFromRequest(request).build();
        String host = uriComponents.getHost();
        host = host != null ? host.toLowerCase() : host;
        MDC.put("domain", domains.getDomainEffectiveName(host));
        requestContext.setOptions(domains.getDomainOptions(host));
        requestContext.setSiteUrl(buildSiteUrl(host, uriComponents.getPort()));
        return true;
    }

    private String buildSiteUrl(String host, int port) {
        switch (port) {
            case 80:
                return String.format("http://%s", host);
            case 443:
                return String.format("https://%s", host);
            default:
                return String.format("http://%s:%d", host, port);
        }
    }

}
