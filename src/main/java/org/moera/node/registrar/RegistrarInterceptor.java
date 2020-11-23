package org.moera.node.registrar;

import java.util.Objects;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.config.Config;
import org.moera.node.global.RequestContext;
import org.moera.node.util.UriUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.springframework.web.util.UriComponents;

@Component
public class RegistrarInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (StringUtils.isEmpty(config.getRegistrar().getHost())) {
            return true;
        }
        UriComponents uriComponents = UriUtil.createBuilderFromRequest(request).build();
        String host = uriComponents.getHost();
        if (Objects.equals(host, config.getRegistrar().getHost())) {
            requestContext.setRegistrar(true);
        }
        return true;
    }

}
