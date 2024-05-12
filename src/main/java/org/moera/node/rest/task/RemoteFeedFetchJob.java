package org.moera.node.rest.task;

import java.util.List;
import javax.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.moera.node.api.node.NodeApiException;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.FeedSliceInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.picker.PickerPool;
import org.moera.node.task.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteFeedFetchJob extends Job<RemoteFeedFetchJob.Parameters, Object> {

    public static class Parameters {

        private String feedName;
        private String remoteNodeName;
        private String remoteFeedName;

        public Parameters() {
        }

        public Parameters(String feedName, String remoteNodeName, String remoteFeedName) {
            this.feedName = feedName;
            this.remoteNodeName = remoteNodeName;
            this.remoteFeedName = remoteFeedName;
        }

        public String getFeedName() {
            return feedName;
        }

        public void setFeedName(String feedName) {
            this.feedName = feedName;
        }

        public String getRemoteNodeName() {
            return remoteNodeName;
        }

        public void setRemoteNodeName(String remoteNodeName) {
            this.remoteNodeName = remoteNodeName;
        }

        public String getRemoteFeedName() {
            return remoteFeedName;
        }

        public void setRemoteFeedName(String remoteFeedName) {
            this.remoteFeedName = remoteFeedName;
        }

    }

    public static final int FETCH_LIMIT = 20;

    private static final Logger log = LoggerFactory.getLogger(RemoteFeedFetchJob.class);

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PickerPool pickerPool;

    public RemoteFeedFetchJob() {
        noRetry();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) throws JsonProcessingException {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
    }

    @Override
    protected void execute() throws NodeApiException {
        FeedSliceInfo sliceInfo = nodeApi.getFeedStories(
                parameters.remoteNodeName,
                parameters.remoteFeedName,
                FETCH_LIMIT);
        log.info("Got {} stories from feed {} at node {}",
                sliceInfo.getStories().size(), parameters.remoteFeedName, parameters.remoteNodeName);
        List<PostingInfo> list = sliceInfo.getStories().stream()
                .filter(t -> t.getStoryType() == StoryType.POSTING_ADDED)
                .filter(t -> !t.isPinned())
                .map(StoryInfo::getPosting)
                .toList();
        for (int i = list.size() - 1; i >= 0; i--) {
            download(list.get(i));
        }
    }

    private void download(PostingInfo postingInfo) {
        String receiverName = postingInfo.isOriginal() ? parameters.remoteNodeName : postingInfo.getReceiverName();
        String receiverPostingId = postingInfo.isOriginal() ? postingInfo.getId() : postingInfo.getReceiverPostingId();
        Posting posting = postingRepository.findByReceiverId(nodeId, receiverName, receiverPostingId).orElse(null);
        if (posting == null) {
            Pick pick = new Pick();
            pick.setNodeId(nodeId);
            pick.setRemoteNodeName(parameters.remoteNodeName);
            pick.setRemoteFeedName(parameters.remoteFeedName);
            pick.setRemotePostingId(postingInfo.getId());
            pick.setFeedName(parameters.feedName);
            pickerPool.pick(pick);
        }
    }

}
