package org.moera.node.rest.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.RecommendedPostingInfo;
import org.moera.lib.node.types.Scope;
import org.moera.node.data.Feed;
import org.moera.node.data.Pick;
import org.moera.node.picker.PickerPool;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

public class FetchRecommendationJob extends Job<FetchRecommendationJob.Parameters, Object> {

    public record Parameters(
        String feedName,
        int limit
    ) {
    }

    private static final Logger log = LoggerFactory.getLogger(FetchRecommendationJob.class);

    @Inject
    private PickerPool pickerPool;

    public FetchRecommendationJob() {
        noRetry();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = null;
    }

    @Override
    protected void execute() throws MoeraNodeException {
        String sourceNode = universalContext.getOptions().getString("recommendations.source");
        boolean safe = universalContext.getOptions().getBool("recommendations.safe");
        String sheriffName = safe ? universalContext.getOptions().getString("recommendations.sheriff") : null;
        int limit = parameters.limit;
        int requestLimit = Math.clamp(Math.max(2L * limit, limit + 5L), Integer.MIN_VALUE, Integer.MAX_VALUE);
        RecommendedPostingInfo[] recommendations = nodeApi
            .at(sourceNode, generateCarte(sourceNode, Scope.IDENTIFY))
            .getRecommendedPostings(parameters.feedName, sheriffName, requestLimit);

        if (ObjectUtils.isEmpty(recommendations)) {
            log.info("No recommendations received");
            return;
        }

        var selected = selectRecommendations(recommendations, limit, ThreadLocalRandom.current());
        for (RecommendedPostingInfo recommendation : selected) {
            log.info(
                "Recommended posting {} at node {}",
                recommendation.getPostingId(), recommendation.getNodeName()
            );

            Pick pick = new Pick();
            pick.setRemoteNodeName(recommendation.getNodeName());
            pick.setRemoteFeedName(Feed.TIMELINE);
            pick.setRemotePostingId(recommendation.getPostingId());
            pick.setFeedName(parameters.feedName);
            pick.setRecommended(true);
            pickerPool.pick(pick);

            nodeApi
                .at(sourceNode, generateCarte(sourceNode, Scope.UPDATE_FEEDS))
                .acceptRecommendedPosting(
                    recommendation.getNodeName(), recommendation.getPostingId(), parameters.feedName
                );
        }
    }

    static List<RecommendedPostingInfo> selectRecommendations(
        RecommendedPostingInfo[] recommendations,
        int limit,
        RandomGenerator random
    ) {
        var candidates = Arrays.asList(recommendations);
        if (candidates.size() <= limit) {
            return candidates;
        }

        var remaining = new ArrayList<>(candidates);
        long totalWeight = remaining.stream().mapToLong(FetchRecommendationJob::recommendationWeight).sum();
        var selected = new ArrayList<RecommendedPostingInfo>(limit);
        for (int i = 0; i < limit; i++) {
            long ticket = random.nextLong(totalWeight);
            int index = 0;
            long weight = recommendationWeight(remaining.get(index));
            while (ticket >= weight) {
                ticket -= weight;
                index++;
                weight = recommendationWeight(remaining.get(index));
            }
            totalWeight -= weight;
            selected.add(remaining.remove(index));
        }
        return selected;
    }

    private static long recommendationWeight(RecommendedPostingInfo recommendation) {
        return Math.max(1L, Objects.requireNonNullElse(recommendation.getBenefit(), 0) + 1L);
    }

}
