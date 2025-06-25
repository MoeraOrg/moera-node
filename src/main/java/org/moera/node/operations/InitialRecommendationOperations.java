package org.moera.node.operations;

import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.data.InitialRecommendationRepository;
import org.moera.node.global.RequestCounter;
import org.moera.node.task.Jobs;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InitialRecommendationOperations {

    private static final Logger log = LoggerFactory.getLogger(InitialRecommendationOperations.class);

    @Inject
    private RequestCounter requestCounter;

    @Inject
    private InitialRecommendationRepository initialRecommendationRepository;

    @Inject
    private Transaction tx;

    @Inject
    private Jobs jobs;

    public void populateNewsfeed(UUID nodeId) {
        jobs.run(PopulateNewsfeedJob.class, new PopulateNewsfeedJob.Parameters(), nodeId);
    }

    @Scheduled(fixedDelayString = "P1D")
    public void refresh() {
        if (!jobs.isReady()) {
            return;
        }

        try (var ignored = requestCounter.allot()) {
            log.info("Refreshing initial recommendations");
            tx.executeWrite(() -> initialRecommendationRepository.deleteExpired(Util.now()));
            jobs.run(FetchInitialRecommendationsJob.class, new FetchInitialRecommendationsJob.Parameters());
        }
    }

}
