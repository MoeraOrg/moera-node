package org.moera.node.rest.task;

import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

public class FetchRecommendationJob extends Job<FetchRecommendationJob.Parameters, Object> {

    public static class Parameters {

        private int limit;

        public Parameters() {
        }

        public Parameters(int limit) {
            this.limit = limit;
        }

        public int getLimit() {
            return limit;
        }

        public void setLimit(int limit) {
            this.limit = limit;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(FetchRecommendationJob.class);

    @Inject
    private PickerPool pickerPool;

    public FetchRecommendationJob() {
        noRetry();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, FetchRecommendationJob.Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
        this.state = null;
    }

    @Override
    protected void execute() throws MoeraNodeException {
        String sourceNode = universalContext.getOptions().getString("recommendations.source");
        boolean safe = universalContext.getOptions().getBool("recommendations.safe");
        String sheriffName = safe ? universalContext.getOptions().getString("recommendations.sheriff") : null;
        RecommendedPostingInfo[] recommendations = nodeApi
            .at(sourceNode, generateCarte(sourceNode, Scope.IDENTIFY))
            .getRecommendedPostings(sheriffName, parameters.limit);

        if (ObjectUtils.isEmpty(recommendations)) {
            log.info("No recommendations received");
            return;
        }

        for (RecommendedPostingInfo recommendation : recommendations) {
            log.info(
                "Recommended posting {} at node {}",
                recommendation.getPostingId(), recommendation.getNodeName()
            );

            Pick pick = new Pick();
            pick.setRemoteNodeName(recommendation.getNodeName());
            pick.setRemoteFeedName(Feed.TIMELINE);
            pick.setRemotePostingId(recommendation.getPostingId());
            pick.setFeedName(Feed.NEWS);
            pick.setRecommended(true);
            pickerPool.pick(pick);

            nodeApi
                .at(sourceNode, generateCarte(sourceNode, Scope.UPDATE_FEEDS))
                .acceptRecommendedPosting(recommendation.getNodeName(), recommendation.getPostingId());
        }
    }

}
