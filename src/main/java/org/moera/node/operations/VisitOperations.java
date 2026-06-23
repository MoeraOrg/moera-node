package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import jakarta.inject.Inject;

import org.moera.lib.node.types.SearchEngine;
import org.moera.lib.node.types.Scope;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.EntryRepository;
import org.moera.node.data.EntryVisitRepository;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SearchEngineStatistics;
import org.moera.node.data.SearchEngineStatisticsRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UserAgent;
import org.moera.node.liberin.model.PostingViewedLiberin;
import org.moera.node.liberin.model.SearchEngineClickedLiberin;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class VisitOperations {

    private static final Logger log = LoggerFactory.getLogger(VisitOperations.class);

    private static final int VISIT_TTL = 1; // days

    private record SearchEngineReferrer(Pattern referrerPattern, UserAgent botUserAgent, SearchEngine engine) {

        SearchEngineReferrer(String referrerPattern, UserAgent botUserAgent, SearchEngine engine) {
            this(Pattern.compile(referrerPattern), botUserAgent, engine);
        }

    }

    private static final SearchEngineReferrer[] SEARCH_ENGINE_REFERRERS = {
        new SearchEngineReferrer("^https?://[a-z]+\\.google\\.com", UserAgent.GOOGLEBOT, SearchEngine.GOOGLE),
        new SearchEngineReferrer(
            "^android-app://com\\.google\\.android\\.googlequicksearchbox", UserAgent.GOOGLEBOT, SearchEngine.GOOGLE
        ),
        new SearchEngineReferrer("^https?://(?:[a-z]+\\.)?bing\\.com", UserAgent.BINGBOT, SearchEngine.BING),
        new SearchEngineReferrer("^https?://ya(?:ndex)?\\.ru", UserAgent.YANDEXBOT, SearchEngine.YANDEX)
    };

    @Inject
    private RequestContext requestContext;

    @Inject
    private SearchEngineStatisticsRepository searchEngineStatisticsRepository;

    @Inject
    private EntryVisitRepository entryVisitRepository;

    @Inject
    private EntryRepository entryRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private Transaction tx;

    public void recordVisit(String postingId, String commentId, String mediaId, String referrer) {
        if (ObjectUtils.isEmpty(requestContext.nodeName())) {
            return;
        }

        String clientName = requestContext.getClientName(Scope.VIEW_CONTENT);
        if (Objects.equals(clientName, requestContext.nodeName())) {
            return;
        }

        if (!ObjectUtils.isEmpty(referrer)) {
            recordSearchEngineVisit(postingId, commentId, mediaId, referrer);
        } else {
            recordEntryVisit(postingId, requestContext.getSessionId(), clientName);
        }
    }

    private void recordSearchEngineVisit(String postingId, String commentId, String mediaId, String referrer) {
        SearchEngine searchEngine = findSearchEngine(referrer);
        if (searchEngine == null) {
            return;
        }

        SearchEngineStatistics searchEngineStatistics = new SearchEngineStatistics();
        searchEngineStatistics.setId(UUID.randomUUID());
        searchEngineStatistics.setNodeId(requestContext.nodeId());
        searchEngineStatistics.setNodeName(requestContext.nodeName());
        searchEngineStatistics.setEngine(searchEngine);
        searchEngineStatistics.setOwnerName(requestContext.nodeName());
        fillTarget(searchEngineStatistics, postingId, commentId, mediaId);

        if (Objects.equals(requestContext.nodeName(), searchEngineStatistics.getOwnerName())) {
            tx.executeWriteQuietly(() -> searchEngineStatisticsRepository.save(searchEngineStatistics));
        } else {
            requestContext.send(new SearchEngineClickedLiberin(searchEngineStatistics));
        }
    }

    private SearchEngine findSearchEngine(String referrer) {
        for (var searchEngine : SEARCH_ENGINE_REFERRERS) {
            if (searchEngine.referrerPattern().matcher(referrer).find()) {
                return requestContext.getUserAgent() == searchEngine.botUserAgent() ? null : searchEngine.engine();
            }
        }
        return null;
    }

    private void fillTarget(
        SearchEngineStatistics searchEngineStatistics, String postingIdS, String commentId, String mediaId
    ) {
        UUID postingId = Util.uuid(postingIdS).orElse(null);
        if (postingId == null) {
            // pass, not a posting
            return;
        }
        Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId).orElse(null);
        if (posting == null) {
            return;
        }

        searchEngineStatistics.setPostingId(postingIdS);
        searchEngineStatistics.setOwnerName(posting.getOwnerName());
        searchEngineStatistics.setHeading(posting.getCurrentRevision().getHeading());

        fillComment(searchEngineStatistics, commentId);
        fillMedia(searchEngineStatistics, mediaId);
    }

    private void fillComment(SearchEngineStatistics searchEngineStatistics, String commentIdS) {
        UUID commentId = Util.uuid(commentIdS).orElse(null);
        if (commentId == null) {
            // pass, not a comment
            return;
        }
        Comment comment = commentRepository.findByNodeIdAndId(requestContext.nodeId(), commentId).orElse(null);
        if (comment == null) {
            return;
        }

        searchEngineStatistics.setCommentId(commentIdS);
        searchEngineStatistics.setOwnerName(comment.getOwnerName());
        searchEngineStatistics.setHeading(comment.getCurrentRevision().getHeading());
    }

    private void fillMedia(SearchEngineStatistics searchEngineStatistics, String mediaId) {
        if (mediaId == null) {
            return;
        }

        searchEngineStatistics.setMediaId(mediaId);
        if (searchEngineStatistics.getHeading() != null) {
            searchEngineStatistics.setHeading(
                HeadingExtractor.EMOJI_PICTURE + ": " + searchEngineStatistics.getHeading()
            );
        } else {
            searchEngineStatistics.setHeading(HeadingExtractor.EMOJI_PICTURE);
        }
    }

    private void recordEntryVisit(String postingIdS, String sessionId, String clientName) {
        UUID postingId = Util.uuid(postingIdS).orElse(null);
        if (postingId == null) {
            // pass, not a posting
            return;
        }

        Timestamp visitedAt = Util.now();
        Timestamp deadline = Timestamp.from(visitedAt.toInstant().plus(VISIT_TTL, ChronoUnit.DAYS));

        tx.executeWriteQuietly(() -> {
            Posting posting = postingRepository.findByNodeIdAndId(requestContext.nodeId(), postingId)
                .orElse(null);
            if (posting != null) {
                int inserted = entryVisitRepository.insertIfAbsent(
                    UUID.randomUUID(),
                    requestContext.nodeId(),
                    posting,
                    ObjectUtils.isEmpty(sessionId) || !ObjectUtils.isEmpty(clientName) ? null : sessionId,
                    ObjectUtils.isEmpty(clientName) ? null : clientName,
                    visitedAt,
                    deadline
                );
                if (inserted > 0) {
                    entryRepository.incrementViewCount(requestContext.nodeId(), postingId);
                    entryRepository.findViewCountByNodeIdAndId(requestContext.nodeId(), postingId)
                        .ifPresent(viewCount -> requestContext.send(new PostingViewedLiberin(postingId, viewCount)));
                }
            }
        });
    }

    @Scheduled(fixedDelayString = "PT1H")
    public void purgeExpired() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging expired entry visits");

            tx.executeWrite(() -> entryVisitRepository.deleteExpired(Util.now()));
        }
    }

}
