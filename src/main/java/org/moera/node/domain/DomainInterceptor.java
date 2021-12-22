package org.moera.node.domain;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.config.Config;
import org.moera.node.config.MultiHost;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.ProviderApi;
import org.moera.node.global.RequestContext;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.util.UriComponents;

@Component
public class DomainInterceptor implements HandlerInterceptor {

    @Inject
    private Config config;

    @Inject
    private Domains domains;

    @Inject
    private RequestContext requestContext;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        if (handler instanceof ResourceHttpRequestHandler) {
            return true;
        }

        UriComponents uriComponents = UriUtil.createBuilderFromRequest(request).build();
        requestContext.setUrl(uriComponents.toUriString());
        String host = uriComponents.getHost();
        host = host != null ? host.toLowerCase() : host;

        if (config.isRegistrarEnabled() && config.getRegistrar().getHost().toLowerCase().equals(host)) {
            requestContext.setRegistrar(true);
            return true;
        }

        if (handler instanceof HandlerMethod && ((HandlerMethod) handler).hasMethodAnnotation(ProviderApi.class)) {
            return true;
        }

        if (host == null || domains.isDomainDefined(host) || config.getMulti() == MultiHost.NONE) {
            MDC.put("domain", domains.getDomainEffectiveName(host));
            requestContext.setOptions(domains.getDomainOptions(host));
            requestContext.setSiteUrl(UriUtil.siteUrl(host, uriComponents.getPort()));
            return true;
        }

        if (request.getDispatcherType() == DispatcherType.ERROR || request.getMethod().equals("OPTIONS")) {
            return true;
        }

        if (config.isRegistrarEnabled()) {
            String hostname = host.substring(0, host.length() - config.getRegistrar().getDomain().length() - 1);
            response.sendRedirect(String.format("%s/registrar?host=%s",
                    UriUtil.siteUrl(config.getRegistrar().getHost(), uriComponents.getPort()), Util.ue(hostname)));
            return false;
        }

        throw new PageNotFoundException();
    }

}
