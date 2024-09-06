package org.moera.node.auth;

import java.util.UUID;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SearchEngine;
import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.data.SearchEngineStatisticsRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.util.Transaction;
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
    private SearchEngineStatisticsRepository searchEngineStatisticsRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private Transaction tx;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (ObjectUtils.isEmpty(requestContext.nodeName())) {
            return true;
        }

        String referer = request.getHeader("Referer");
        if (ObjectUtils.isEmpty(referer)) {
            return true;
        }

        SearchEngine searchEngine = findSearchEngine(referer);
        if (searchEngine == null) {
            return true;
        }

        SearchEngineStatistics searchEngineStatistics = new SearchEngineStatistics();
        searchEngineStatistics.setId(UUID.randomUUID());
        searchEngineStatistics.setNodeName(requestContext.nodeName());
        searchEngineStatistics.setEngine(searchEngine);
        searchEngineStatistics.setOwnerName(requestContext.nodeName());

        UriComponents uric = UriComponentsBuilder
                .fromUriString(request.getRequestURI())
                .query(request.getQueryString())
                .build();
        if (uric.getPathSegments().size() >= 2 && uric.getPathSegments().get(0).equals("post")) {
            try {
                UUID postingId = UUID.fromString(uric.getPathSegments().get(1));
                Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
                if (posting != null) {
                    searchEngineStatistics.setPostingId(postingId);
                    searchEngineStatistics.setOwnerName(posting.getOwnerName());

                    String commentS = uric.getQueryParams().getFirst("comment");
                    if (commentS != null) {
                        try {
                            UUID commentId = UUID.fromString(commentS);
                            Comment comment = commentRepository.findByNodeIdAndId(requestContext.nodeId(), commentId)
                                    .orElse(null);
                            if (comment != null) {
                                searchEngineStatistics.setCommentId(commentId);
                                searchEngineStatistics.setOwnerName(comment.getOwnerName());
                            }
                        } catch (IllegalArgumentException e) {
                            // pass, not a comment
                        }
                    }

                    String mediaS = uric.getQueryParams().getFirst("media");
                    if (mediaS != null) {
                        try {
                            searchEngineStatistics.setMediaId(UUID.fromString(mediaS));
                        } catch (IllegalArgumentException e) {
                            // pass, not a media
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // pass, not a posting
            }
        }

        tx.executeWriteQuietly(() -> searchEngineStatisticsRepository.save(searchEngineStatistics));

        return true;
    }

    private SearchEngine findSearchEngine(String referer) {
        for (SearchEngine searchEngine : SearchEngine.values()) {
            if (searchEngine.getRefererPattern().matcher(referer).find()) {
                return requestContext.getUserAgent() == searchEngine.getBotUserAgent() ? null : searchEngine;
            }
        }
        return null;
    }

}
