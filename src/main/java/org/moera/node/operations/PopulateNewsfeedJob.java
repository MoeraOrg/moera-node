package org.moera.node.operations;

import java.util.List;
import jakarta.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) throws JsonProcessingException {
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

        List<InitialRecommendation> all = tx.executeRead(() -> initialRecommendationRepository.findAll());
        for (InitialRecommendation recommendation : all) {
            Pick pick = new Pick();
            pick.setRemoteNodeName(recommendation.getNodeName());
            pick.setRemoteFeedName(Feed.TIMELINE);
            pick.setRemotePostingId(recommendation.getPostingId());
            pick.setFeedName(Feed.NEWS);
            pick.setRecommended(true);
            pickerPool.pick(pick);
        }
    }

}
