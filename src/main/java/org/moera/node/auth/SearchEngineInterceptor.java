package org.moera.node.auth;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.moera.node.global.RequestContext;
import org.moera.node.operations.VisitOperations;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SearchEngineInterceptor implements HandlerInterceptor {

    @Inject
    private RequestContext requestContext;

    @Inject
    private VisitOperations visitOperations;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (ObjectUtils.isEmpty(requestContext.nodeName())) {
            return true;
        }

        String referer = request.getHeader("Referer");
        if (ObjectUtils.isEmpty(referer)) {
            return true;
        }

        UriComponents uric = UriComponentsBuilder
                .fromUriString(request.getRequestURI())
                .query(request.getQueryString())
                .build();
        String postingId = null;
        String commentId = null;
        String mediaId = null;
        if (uric.getPathSegments().size() >= 2 && uric.getPathSegments().get(0).equals("post")) {
            postingId = uric.getPathSegments().get(1);
            commentId = uric.getQueryParams().getFirst("comment");
            mediaId = uric.getQueryParams().getFirst("media");
        }

        visitOperations.recordVisit(postingId, commentId, mediaId, referer);
        return true;
    }

}
