package org.moera.node.global;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.util.VirtualPageHeader;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

@Component
public class VirtualPageInterceptor extends HandlerInterceptorAdapter {

    @Inject
    private RequestContext requestContext;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {

        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        VirtualPage virtualPage = ((HandlerMethod) handler).getMethodAnnotation(VirtualPage.class);
        if (virtualPage == null) {
            return true;
        }
        VirtualPageHeader.put(response, virtualPage.value());

        if (requestContext.isBrowserExtension()) {
            response.setContentType("text/html; charset=utf-8");
            Writer out = new OutputStreamWriter(response.getOutputStream());
            out.write("<!DOCTYPE html>\n"
                    + "<html>\n"
                    + "    <head>\n"
                    + "        <meta name='viewport' content='width=device-width, initial-scale=1, shrink-to-fit=no'>\n"
                    + "        <title>Moera</title>\n"
                    + "    </head>\n"
                    + "    <body></body>\n"
                    + "</html>\n");
            out.close();
            return false;
        }

        return true;
    }

}
