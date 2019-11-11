package org.moera.node.global;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class ClientIdInterceptor extends HandlerInterceptorAdapter {

    private static Logger log = LoggerFactory.getLogger(ClientIdInterceptor.class);

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String cid = request.getParameter("cid");
        requestContext.setClientId(StringUtils.isEmpty(cid) ? null : cid);

        return true;
    }

}
