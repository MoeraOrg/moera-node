package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.StoryRepository;
import org.moera.node.domain.Domains;
import org.moera.node.global.RequestCounter;
import org.moera.node.global.UniversalContext;
import org.moera.node.option.type.enums.RecommendationFrequency;
import org.moera.node.rest.task.FetchRecommendationJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecommendationOperations {

    private static final Logger log = LoggerFactory.getLogger(RecommendationOperations.class);

    private static final int MIN_INTERVAL = 15;
    private static final int MAX_INTERVAL = 60;
    private static final int INTRODUCTION_INTERVAL = 6 * 60;

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    @Scheduled(fixedDelayString = "PT15M")
    public void checkRecommendations() {
        try (var ignored = requestCounter.allot()) {
            log.info("Checking if recommendations are needed");

            for (String domainName : domains.getWarmDomainNames()) {
                universalContext.associate(domains.getDomainNodeId(domainName));
                var freq = RecommendationFrequency.forValue(
                    universalContext.getOptions().getString("recommendations.frequency")
                );
                if (freq == null || freq == RecommendationFrequency.NONE) {
                    continue;
                }
                int lastDay = tx.executeRead(() ->
                    storyRepository.countLastNotRecommendedPostings(
                        universalContext.nodeId(),
                        Feed.NEWS,
                        Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS))
                    )
                );
                int wantedInterval = lastDay > 0 ? 24 * 60 / lastDay * 2 / 3 : MAX_INTERVAL;
                log.debug("{} normal posts last day, wanted interval is {} mins", lastDay, wantedInterval);
                if (wantedInterval > MIN_INTERVAL / 2) {
                    // Filling mode: add more recommendations to fill the feed
                    if (wantedInterval > MAX_INTERVAL) {
                        wantedInterval = MAX_INTERVAL;
                    }
                    if (wantedInterval < MIN_INTERVAL) {
                        wantedInterval = MIN_INTERVAL;
                    }
                    wantedInterval = Math.round(wantedInterval / freq.getFactor());
                    log.debug("Filling mode with wanted interval {} mins", wantedInterval);
                    int last6Hours = tx.executeRead(() ->
                        storyRepository.countLastPostings(
                            universalContext.nodeId(),
                            Feed.NEWS,
                            Timestamp.from(Instant.now().minus(6, ChronoUnit.HOURS))
                        )
                    );
                    if (last6Hours > 0) {
                        int lastInterval = 6 * 60 / last6Hours;
                        log.debug("{} total posts last 6 hours, actual interval is {} mins", last6Hours, lastInterval);
                        if (lastInterval <= wantedInterval) {
                            continue;
                        }
                    }
                    Timestamp last = tx.executeRead(() ->
                        storyRepository.findLastPostingCreatedAt(universalContext.nodeId(), Feed.NEWS)
                    );
                    if (
                        last != null
                        && last.after(Timestamp.from(Instant.now().minus(wantedInterval, ChronoUnit.MINUTES)))
                    ) {
                        log.debug("Last posting was published recently, skipping this time");
                        continue;
                    }
                } else {
                    // Introduction mode: add several recommendations per day to find new connections
                    wantedInterval = Math.round(INTRODUCTION_INTERVAL / freq.getFactor());
                    log.debug("Introduction mode with wanted interval {} mins", wantedInterval);
                    Timestamp last = tx.executeRead(() ->
                        storyRepository.findLastRecommendedPostingCreatedAt(universalContext.nodeId(), Feed.NEWS)
                    );
                    if (
                        last != null
                        && last.after(Timestamp.from(Instant.now().minus(wantedInterval, ChronoUnit.MINUTES)))
                    ) {
                        log.debug("Recommendation interval is not reached yet, skipping this time");
                        continue;
                    }
                }
                jobs.run(
                    FetchRecommendationJob.class,
                    new FetchRecommendationJob.Parameters(1),
                    universalContext.nodeId()
                );
            }
        }
    }

}
