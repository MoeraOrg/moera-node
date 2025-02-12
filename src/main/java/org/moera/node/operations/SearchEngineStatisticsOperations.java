package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.UniversalLocation;
import org.moera.lib.node.types.StorySummaryData;
import org.moera.lib.node.types.StorySummaryPageClicks;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Comment;
import org.moera.node.data.CommentRepository;
import org.moera.node.data.Feed;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.SearchEngineClicks;
import org.moera.node.data.SearchEngineStatisticsRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.model.StorySummaryPageClicksUtil;
import org.moera.node.text.HeadingExtractor;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class SearchEngineStatisticsOperations {

    private static final Logger log = LoggerFactory.getLogger(SearchEngineStatisticsOperations.class);

    private static final int REPORT_LINES = 15;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private Domains domains;

    @Inject
    private SearchEngineStatisticsRepository searchEngineStatisticsRepository;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private BlockedInstantOperations blockedInstantOperations;

    @Inject
    private Transaction tx;

    @Scheduled(fixedDelayString = "PT6H")
    public void generateReports() {
        try (var ignored = requestCounter.allot()) {
            log.info("Generating search engine statistics reports");

            for (String domainName : domains.getWarmDomainNames()) {
                try {
                    String ownerName = domains.getDomainOptions(domainName).nodeName();
                    if (ObjectUtils.isEmpty(ownerName)) {
                        continue;
                    }

                    UUID nodeId = domains.getDomainNodeId(domainName);
                    universalContext.associate(nodeId);

                    if (blockedInstantOperations.count(nodeId, StoryType.SEARCH_REPORT) > 0) {
                        log.debug("Generation of reports for domain {} blocked", LogUtil.format(domainName));
                        continue;
                    }

                    Timestamp lastReport = domains.getDomainOptions(domainName)
                            .getTimestamp("search-engines.report.generated");
                    if (lastReport == null) {
                        lastReport = Util.toTimestamp(domains.getDomain(domainName).getCreatedAt());
                    }
                    if (lastReport.toInstant().plus(7, ChronoUnit.DAYS).isAfter(Instant.now())) {
                        continue;
                    }

                    log.info("Generating report for domain {}", LogUtil.format(domainName));

                    if (lastReport.toInstant().isBefore(Instant.now().minus(10, ChronoUnit.DAYS))) {
                        lastReport = Timestamp.from(Instant.now().minus(7, ChronoUnit.DAYS));
                    }
                    Timestamp prevReport = lastReport;
                    Timestamp currentReport = Util.now();

                    List<SearchEngineClicks> clicks = tx.executeRead(() ->
                            searchEngineStatisticsRepository.calculateClicks(ownerName, prevReport, currentReport,
                                    PageRequest.of(0, REPORT_LINES)));
                    if (ObjectUtils.isEmpty(clicks)) {
                        domains.getDomainOptions(domainName).set("search-engines.report.generated", currentReport);
                        continue;
                    }

                    Story story = new Story(UUID.randomUUID(), nodeId, StoryType.SEARCH_REPORT);
                    story.setFeedName(Feed.NEWS);
                    StorySummaryData summaryData = new StorySummaryData();
                    summaryData.setClicks(clicks.stream().map(this::toSummary).toList());
                    story.setSummaryData(summaryData);
                    Story savedStory = tx.executeWrite(() -> {
                        storyOperations.updateMoment(story, nodeId);
                        return storyRepository.saveAndFlush(story);
                    });
                    universalContext.send(new StoryAddedLiberin(savedStory));

                    domains.getDomainOptions(domainName).set("search-engines.report.generated", currentReport);
                } catch (Exception e) {
                    log.error("Exception while generating report", e);
                }
            }
        }
    }

    private StorySummaryPageClicks toSummary(SearchEngineClicks clicks) {
        String heading = getSummaryHeading(clicks);
        String href = getSummaryHref(clicks);
        return StorySummaryPageClicksUtil.build(heading, href, (int) clicks.getClicks());
    }

    private String getSummaryHeading(SearchEngineClicks clicks) {
        String heading = clicks.getHeading();
        if (heading == null && clicks.getPostingId() != null) {
            String nodeDomain = domains.findDomainByNodeName(clicks.getNodeName());
            if (nodeDomain != null) {
                UUID nodeId = domains.getDomainNodeId(nodeDomain);
                heading = tx.executeRead(() -> {
                    if (clicks.getCommentId() != null) {
                        UUID commentId = UUID.fromString(clicks.getCommentId());
                        Comment comment = commentRepository.findFullByNodeIdAndId(nodeId, commentId)
                                .orElse(null);
                        if (comment != null) {
                            return comment.getCurrentRevision().getHeading();
                        }
                    }
                    UUID postingId = UUID.fromString(clicks.getPostingId());
                    Posting posting = postingRepository.findFullByNodeIdAndId(nodeId, postingId)
                            .orElse(null);
                    return posting != null ? posting.getCurrentRevision().getHeading() : null;
                });
            }
            if (heading == null) {
                heading = "";
            }
        }
        if (clicks.getMediaId() != null) {
            heading = HeadingExtractor.EMOJI_PICTURE + ": " + heading;
        }
        return heading;
    }

    private String getSummaryHref(SearchEngineClicks clicks) {
        String path;
        String query;
        if (clicks.getPostingId() != null) {
            path = String.format("/post/%s", clicks.getPostingId());
            if (clicks.getCommentId() != null) {
                if (clicks.getMediaId() != null) {
                    query = String.format("comment=%s&media=%s", clicks.getCommentId(), clicks.getMediaId());
                } else {
                    query = String.format("comment=%s", clicks.getCommentId());
                }
            } else {
                if (clicks.getMediaId() != null) {
                    query = String.format("media=%s", clicks.getMediaId());
                } else {
                    query = null;
                }
            }
        } else {
            path = "/";
            query = null;
        }

        return UniversalLocation.redirectTo(clicks.getNodeName(), null, path, query, null);
    }

    @Scheduled(fixedDelayString = "P1D")
    public void purgeOutdated() {
        try (var ignored = requestCounter.allot()) {
            log.info("Purging outdated search engine statistics");

            Timestamp before = Timestamp.from(Instant.now().minus(14, ChronoUnit.DAYS));
            tx.executeWrite(() -> searchEngineStatisticsRepository.deleteOutdated(before));
        }
    }

}
