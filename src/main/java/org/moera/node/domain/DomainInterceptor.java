package org.moera.node.domain;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.config.Config;
import org.moera.node.global.RequestContext;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.UriComponents;

@Component
public class DomainInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private Config config;

    @Inject
    private Domains domains;

    @Inject
    private RequestContext requestContext;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        UriComponents uriComponents = UriUtil.createBuilderFromRequest(request).build();
        requestContext.setUrl(uriComponents.toUriString());
        if (requestContext.isRegistrar()) {
            return true;
        }
        String host = uriComponents.getHost();
        host = host != null ? host.toLowerCase() : host;
        if (host == null || domains.isDomainDefined(host) || StringUtils.isEmpty(config.getRegistrar().getDomain())) {
            MDC.put("domain", domains.getDomainEffectiveName(host));
            requestContext.setOptions(domains.getDomainOptions(host));
            requestContext.setSiteUrl(UriUtil.siteUrl(host, uriComponents.getPort()));
            return true;
        } else if (!StringUtils.isEmpty(config.getRegistrar().getHost())) {
            String hostname = host.substring(0, host.length() - config.getRegistrar().getDomain().length() - 1);
            response.sendRedirect(String.format("%s/registrar?host=%s",
                    UriUtil.siteUrl(config.getRegistrar().getHost(), uriComponents.getPort()), Util.ue(hostname)));
            return false;
        }
        return true;
    }

}
