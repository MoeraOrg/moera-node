package org.moera.node.global;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class CacheControlInterceptor extends HandlerInterceptorAdapter {

    private static final String X_ACCEPT_MOERA = "X-Accept-Moera";

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {

        if (!(handler instanceof HandlerMethod)) {
            return;
        }
        Class<?> controllerType = ((HandlerMethod) handler).getBeanType();
        if (!AnnotatedElementUtils.hasAnnotation(controllerType, UiController.class)) {
            return;
        }
        if (request.getHeader(X_ACCEPT_MOERA) != null) {
            response.addHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
        }
        response.addHeader(HttpHeaders.VARY, X_ACCEPT_MOERA);
    }

}
