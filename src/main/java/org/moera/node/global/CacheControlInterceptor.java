package org.moera.node.global;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CacheControlInterceptor implements HandlerInterceptor {

    private static final String X_ACCEPT_MOERA = "X-Accept-Moera";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        Class<?> controllerType = ((HandlerMethod) handler).getBeanType();
        Method methodType = ((HandlerMethod) handler).getMethod();
        if (AnnotatedElementUtils.hasAnnotation(controllerType, UiController.class)) {
            if (AnnotatedElementUtils.hasAnnotation(controllerType, MaxCache.class)
                    || AnnotatedElementUtils.hasAnnotation(methodType, MaxCache.class)) {
                response.addHeader(HttpHeaders.CACHE_CONTROL,
                        CacheControl.maxAge(3650, TimeUnit.DAYS).getHeaderValue());
            } else if (request.getHeader(X_ACCEPT_MOERA) != null) {
                response.addHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
            }
            response.addHeader(HttpHeaders.VARY, X_ACCEPT_MOERA);
        } else {
            if (AnnotatedElementUtils.hasAnnotation(controllerType, NoCache.class)
                || AnnotatedElementUtils.hasAnnotation(methodType, NoCache.class)) {
                response.addHeader(HttpHeaders.CACHE_CONTROL, CacheControl.noStore().getHeaderValue());
            }
        }
        return true;
    }

}
