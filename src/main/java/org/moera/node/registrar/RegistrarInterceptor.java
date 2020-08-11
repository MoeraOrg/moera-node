package org.moera.node.registrar;

import java.util.Objects;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.util.UriUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.UriComponents;

@Component
public class RegistrarInterceptor extends HandlerInterceptorAdapter {

    @Value("${registrar.host}")
    private String registrarHost;

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (StringUtils.isEmpty(registrarHost)) {
            return true;
        }
        UriComponents uriComponents = UriUtil.createBuilderFromRequest(request).build();
        String host = uriComponents.getHost();
        if (Objects.equals(host, registrarHost)) {
            requestContext.setRegistrar(true);
        }
        return true;
    }

}
