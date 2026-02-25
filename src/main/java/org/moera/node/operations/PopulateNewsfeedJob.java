package org.moera.node.operations;

import java.util.List;
import jakarta.inject.Inject;

import org.moera.lib.naming.types.OperationStatus;
import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.node.data.Feed;
import org.moera.node.data.InitialRecommendation;
import org.moera.node.data.InitialRecommendationRepository;
import org.moera.node.data.Pick;
import org.moera.node.picker.PickerPool;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class PopulateNewsfeedJob extends Job<PopulateNewsfeedJob.Parameters, Object> {

    public static class Parameters {
    }

    private static final Logger log = LoggerFactory.getLogger(PopulateNewsfeedJob.class);

    @Inject
    private InitialRecommendationRepository initialRecommendationRepository;

    @Inject
    private PickerPool pickerPool;

    public PopulateNewsfeedJob() {
        retryCount(120, "PT5S");
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
        if (universalContext.nodeName() == null) {
            log.info("Node name is not assigned yet, waiting");
            retry();
        }

        OperationStatus status = OperationStatus.forValue(
            universalContext.getOptions().getString("naming.operation.status")
        );
        if (status == OperationStatus.WAITING || status == OperationStatus.ADDED || status == OperationStatus.STARTED) {
            log.info("Node name registration is pending, waiting");
            retry();
        }
        if (status == OperationStatus.FAILED) {
            log.info("Node name registration failed, giving up");
            fail();
        }

        List<InitialRecommendation> all = tx.executeRead(() -> initialRecommendationRepository.findAllBackwards());
        all.stream().map(r -> createPick(r, Feed.NEWS)).forEach(pickerPool::pick);
        all.stream().map(r -> createPick(r, Feed.EXPLORE)).forEach(pickerPool::pick);
    }

    private static Pick createPick(InitialRecommendation recommendation, String feedName) {
        Pick pick = new Pick();
        pick.setRemoteNodeName(recommendation.getNodeName());
        pick.setRemoteFeedName(Feed.TIMELINE);
        pick.setRemotePostingId(recommendation.getPostingId());
        pick.setFeedName(feedName);
        pick.setRecommended(true);
        pick.setViewed(true);
        pick.setPublishAt(recommendation.getCreatedAt());
        return pick;
    }

}
