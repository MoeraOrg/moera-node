package org.moera.node.auth;

import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.moera.lib.node.types.SearchEngine;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.data.SearchEngineStatisticsRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UserAgent;
import org.moera.node.liberin.model.SearchEngineClickedLiberin;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.util.Transaction;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SearchEngineInterceptor implements HandlerInterceptor {

    private record SearchEngineReferer(Pattern refererPattern, UserAgent botUserAgent, SearchEngine engine) {

        SearchEngineReferer(String refererPattern, UserAgent botUserAgent, SearchEngine engine) {
            this(Pattern.compile(refererPattern), botUserAgent, engine);
        }

    }

    private static final SearchEngineReferer[] SEARCH_ENGINE_REFERERS = {
        new SearchEngineReferer("^https?://[a-z]+\\.google\\.com", UserAgent.GOOGLEBOT, SearchEngine.GOOGLE)
    };

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
                    searchEngineStatistics.setPostingId(postingId.toString());
                    searchEngineStatistics.setOwnerName(posting.getOwnerName());
                    searchEngineStatistics.setHeading(posting.getCurrentRevision().getHeading());

                    String commentS = uric.getQueryParams().getFirst("comment");
                    if (commentS != null) {
                        try {
                            UUID commentId = UUID.fromString(commentS);
                            Comment comment = commentRepository.findByNodeIdAndId(requestContext.nodeId(), commentId)
                                    .orElse(null);
                            if (comment != null) {
                                searchEngineStatistics.setCommentId(commentId.toString());
                                searchEngineStatistics.setOwnerName(comment.getOwnerName());
                                searchEngineStatistics.setHeading(comment.getCurrentRevision().getHeading());
                            }
                        } catch (IllegalArgumentException e) {
                            // pass, not a comment
                        }
                    }

                    String mediaS = uric.getQueryParams().getFirst("media");
                    if (mediaS != null) {
                        searchEngineStatistics.setMediaId(mediaS);
                        if (searchEngineStatistics.getHeading() != null) {
                            searchEngineStatistics.setHeading(
                                    HeadingExtractor.EMOJI_PICTURE + ": " + searchEngineStatistics.getHeading());
                        } else {
                            searchEngineStatistics.setHeading(HeadingExtractor.EMOJI_PICTURE);
                        }
                    }
                }
            } catch (IllegalArgumentException e) {
                // pass, not a posting
            }
        }

        if (Objects.equals(requestContext.nodeName(), searchEngineStatistics.getOwnerName())) {
            tx.executeWriteQuietly(() -> searchEngineStatisticsRepository.save(searchEngineStatistics));
        } else {
            requestContext.send(new SearchEngineClickedLiberin(searchEngineStatistics));
        }

        return true;
    }

    private SearchEngine findSearchEngine(String referer) {
        for (var searchEngine : SEARCH_ENGINE_REFERERS) {
            if (searchEngine.refererPattern().matcher(referer).find()) {
                return requestContext.getUserAgent() == searchEngine.botUserAgent() ? null : searchEngine.engine();
            }
        }
        return null;
    }

}
