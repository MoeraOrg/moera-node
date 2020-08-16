package org.moera.node.domain;

import java.io.IOException;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.util.UriUtil;
import org.moera.node.util.Util;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.UriComponents;

@Component
public class DomainInterceptor extends HandlerInterceptorAdapter {

    @Value("${registrar.host}")
    private String registrarHost;

    @Value("${registrar.domain}")
    private String registrarDomain;

    @Inject
    private Domains domains;

    @Inject
    private RequestContext requestContext;

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        if (requestContext.isRegistrar()) {
            return true;
        }
        UriComponents uriComponents = UriUtil.createBuilderFromRequest(request).build();
        String host = uriComponents.getHost();
        host = host != null ? host.toLowerCase() : host;
        if (host == null || domains.isDomainDefined(host) || StringUtils.isEmpty(registrarDomain)) {
            MDC.put("domain", domains.getDomainEffectiveName(host));
            requestContext.setOptions(domains.getDomainOptions(host));
            requestContext.setSiteUrl(UriUtil.siteUrl(host, uriComponents.getPort()));
            return true;
        } else {
            String hostname = host.substring(0, host.length() - registrarDomain.length() - 1);
            response.sendRedirect(String.format("%s/registrar?host=%s",
                    UriUtil.siteUrl(registrarHost, uriComponents.getPort()), Util.ue(hostname)));
            return false;
        }
    }

}
