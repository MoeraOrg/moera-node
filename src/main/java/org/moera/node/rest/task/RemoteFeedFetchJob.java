package org.moera.node.rest.task;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeException;
import org.moera.lib.node.types.FeedSliceInfo;
import org.moera.lib.node.types.PostingInfo;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.node.data.Pick;
import org.moera.node.data.Posting;
import org.moera.node.data.PostingRepository;
import org.moera.node.data.StoryRepository;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.picker.PickerPool;
import org.moera.node.task.Job;
import org.moera.node.util.SafeInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class RemoteFeedFetchJob extends Job<RemoteFeedFetchJob.Parameters, Object> {

    public record Parameters(
        String feedName,
        String remoteNodeName,
        String remoteFeedName
    ) {
    }

    private record PostingKey(String nodeName, String postingId) {
    }

    public static final int FETCH_LIMIT = 5;

    private static final Logger log = LoggerFactory.getLogger(RemoteFeedFetchJob.class);

    @Inject
    private PostingRepository postingRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private PickerPool pickerPool;

    public RemoteFeedFetchJob() {
        noRetry();
    }

    @Override
    protected void setParameters(String parameters, ObjectMapper objectMapper) {
        this.parameters = objectMapper.readValue(parameters, Parameters.class);
    }

    @Override
    protected void setState(String state, ObjectMapper objectMapper) {
    }

    @Override
    protected void execute() throws MoeraNodeException {
        int existingCount = tx.executeRead(() -> postingRepository.findByOwnerNameAndFeed(
            nodeId, parameters.remoteNodeName, parameters.feedName
        ).size());
        int limit = FETCH_LIMIT - existingCount;
        if (limit <= 0) {
            return;
        }

        List<PostingInfo> list = new ArrayList<>();
        Set<PostingKey> seen = new HashSet<>();
        Long before = null;
        while (list.size() < limit) {
            FeedSliceInfo sliceInfo = nodeApi
                .at(parameters.remoteNodeName)
                .getFeedSlice(parameters.remoteFeedName, null, before, limit - list.size());
            log.info(
                "Got {} stories from feed {} at node {}",
                sliceInfo.getStories().size(), parameters.remoteFeedName, parameters.remoteNodeName
            );
            for (StoryInfo storyInfo : sliceInfo.getStories()) {
                if (storyInfo.getStoryType() != StoryType.POSTING_ADDED || Boolean.TRUE.equals(storyInfo.getPinned())) {
                    continue;
                }
                PostingInfo postingInfo = storyInfo.getPosting();
                PostingKey key = postingKey(postingInfo);
                if (key.nodeName().equals(nodeName())) {
                    continue;
                }
                if (seen.add(key) && !isInFeed(key)) {
                    list.add(postingInfo);
                }
                if (list.size() == limit) {
                    break;
                }
            }
            if (
                sliceInfo.getTotalInPast() == 0
                || sliceInfo.getAfter() <= SafeInteger.MIN_VALUE
                || before != null && sliceInfo.getAfter() >= before
            ) {
                break;
            }
            before = sliceInfo.getAfter();
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            download(list.get(i));
        }
    }

    private PostingKey postingKey(PostingInfo postingInfo) {
        boolean original = PostingInfoUtil.isOriginal(postingInfo);
        return new PostingKey(
            original ? parameters.remoteNodeName : postingInfo.getReceiverName(),
            original ? postingInfo.getId() : postingInfo.getReceiverPostingId()
        );
    }

    private boolean isInFeed(PostingKey key) {
        return tx.executeRead(() -> {
            Posting posting = postingRepository.findByReceiverId(nodeId, key.nodeName(), key.postingId()).orElse(null);
            return posting != null && storyRepository.countByFeedAndTypeAndEntryId(
                nodeId, parameters.feedName, StoryType.POSTING_ADDED, posting.getId()
            ) > 0;
        });
    }

    private void download(PostingInfo postingInfo) {
        Pick pick = new Pick();
        pick.setNodeId(nodeId);
        pick.setRemoteNodeName(parameters.remoteNodeName);
        pick.setRemoteFeedName(parameters.remoteFeedName);
        pick.setRemotePostingId(postingInfo.getId());
        pick.setFeedName(parameters.feedName);
        pickerPool.pick(pick);
    }

}
