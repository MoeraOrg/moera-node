package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.MediaAttachment;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.PrivateMediaFileInfo;
import org.moera.lib.node.types.RecommendedPostingInfo;
import org.moera.node.data.InitialRecommendation;
import org.moera.node.data.InitialRecommendationRepository;
import org.moera.node.media.MediaManager;
import org.moera.node.option.OptionsMetadata;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

public class FetchInitialRecommendationsJob
        extends Job<FetchInitialRecommendationsJob.Parameters, FetchInitialRecommendationsJob.State> {

    public static class Parameters {
    }

    public static class State {

        private RecommendedPostingInfo[] recommendations;

        public State() {
        }

        public State(RecommendedPostingInfo[] recommendations) {
            this.recommendations = recommendations;
        }

        public RecommendedPostingInfo[] getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(RecommendedPostingInfo[] recommendations) {
            this.recommendations = recommendations;
        }

    }

    private static final int BATCH_SIZE = 10;
    private static final Duration TTL = Duration.ofDays(30);

    private static final Logger log = LoggerFactory.getLogger(FetchInitialRecommendationsJob.class);

    @Inject
    private OptionsMetadata optionsMetadata;

    @Inject
    private InitialRecommendationRepository initialRecommendationRepository;

    @Inject
    private MediaManager mediaManager;

    public FetchInitialRecommendationsJob() {
        state = new State();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
        this.state = objectMapper.readValue(state, State.class);
    }

    @Override
    protected void execute() throws MoeraNodeException {
        String sourceNode = optionsMetadata.getDefaultString("recommendations.source");
        boolean safe = optionsMetadata.getDefaultBool("recommendations.safe");
        String sheriffName = safe ? optionsMetadata.getDefaultString("recommendations.sheriff") : null;
        int maxSize = Math.min(
            optionsMetadata.getDefaultInt("media.max-size"),
            optionsMetadata.getDefaultInt("posting.media.max-size")
        );

        state.recommendations = nodeApi
            .at(sourceNode)
            .getRecommendedPostings(null, sheriffName, BATCH_SIZE);

        if (ObjectUtils.isEmpty(state.recommendations)) {
            log.info("No recommendations received");
            return;
        }

        checkpoint();

        for (RecommendedPostingInfo recommendation : state.recommendations) {
            String nodeName = recommendation.getNodeName();
            String postingId = recommendation.getPostingId();
            log.info("Recommended posting {} at node {}", postingId, nodeName);

            InitialRecommendation record = tx.executeRead(() ->
                initialRecommendationRepository
                    .findByNodeNameAndPostingId(nodeName, postingId)
                    .orElse(null)
            );
            if (record != null) {
                continue;
            }

            PostingInfo postingInfo = nodeApi
                .at(nodeName)
                .getPosting(postingId, false);
            for (MediaAttachment attach : postingInfo.getMedia()) {
                PrivateMediaFileInfo info = attach.getMedia();
                mediaManager.downloadPrivateMediaForCaching(
                    nodeName, null, info.getId(), info.getHash(), info.getTextContent(), maxSize
                );
            }

            record = new InitialRecommendation();
            record.setId(UUID.randomUUID());
            record.setNodeName(nodeName);
            record.setPostingId(postingId);
            record.setDeadline(Timestamp.from(Instant.now().plus(TTL)));
            InitialRecommendation savedRecord = record;
            tx.executeWrite(() -> initialRecommendationRepository.save(savedRecord));
        }
    }

}
