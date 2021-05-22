package org.moera.node.rest.task;

import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryType;
import org.moera.node.model.FeedSliceInfo;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryInfo;
import org.moera.node.picker.PickerPool;
import org.moera.node.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteFeedFetchTask extends Task {

    public static final int FETCH_LIMIT = 20;

    private static Logger log = LoggerFactory.getLogger(RemoteFeedFetchTask.class);

    private String feedName;
    private String remoteNodeName;
    private String remoteFeedName;

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private PickerPool pickerPool;

    public RemoteFeedFetchTask(String feedName, String remoteNodeName, String remoteFeedName) {
        this.feedName = feedName;
        this.remoteNodeName = remoteNodeName;
        this.remoteFeedName = remoteFeedName;
    }

    @Override
    protected void execute() {
        try {
            FeedSliceInfo sliceInfo = nodeApi.getFeedStories(remoteNodeName, remoteFeedName, FETCH_LIMIT);
            log.info("Got {} stories from feed {} at node {}", sliceInfo.getStories().size(), remoteFeedName,
                    remoteNodeName);
            List<PostingInfo> list = sliceInfo.getStories().stream()
                    .filter(t -> t.getStoryType() == StoryType.POSTING_ADDED)
                    .filter(t -> !t.isPinned())
                    .map(StoryInfo::getPosting)
                    .collect(Collectors.toList());
            for (int i = list.size() - 1; i >= 0; i--) {
                download(list.get(i));
            }
        } catch (Exception e) {
            log.error("Error downloading stories from feed {} at node {}: {}", remoteFeedName, remoteNodeName,
                    e.getMessage());
        }
    }

    private void download(PostingInfo postingInfo) {
        String receiverName = postingInfo.isOriginal() ? remoteNodeName : postingInfo.getReceiverName();
        String receiverPostingId = postingInfo.isOriginal() ? postingInfo.getId() : postingInfo.getReceiverPostingId();
        Posting posting = postingRepository.findByReceiverId(nodeId, receiverName, receiverPostingId).orElse(null);
        if (posting == null) {
            Pick pick = new Pick();
            pick.setNodeId(nodeId);
            pick.setRemoteNodeName(remoteNodeName);
            pick.setRemoteFeedName(remoteFeedName);
            pick.setRemotePostingId(postingInfo.getId());
            pick.setFeedName(feedName);
            pickerPool.pick(pick);
        }
    }

}
