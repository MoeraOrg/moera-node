package org.moera.node.push;

import java.util.UUID;

import org.moera.lib.node.types.FeedStatus;
import org.moera.lib.node.types.PushContent;
import org.moera.lib.node.types.PushContentType;
import org.moera.lib.node.types.principal.AccessCheckers;
import org.moera.node.data.Story;
import org.moera.node.model.FeedWithStatusUtil;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.StoryInfoUtil;
import org.moera.node.operations.MediaAttachmentsProvider;

public class PushContentBuilder {

    public static PushContent storyAdded(Story story) {
        PushContent packet = new PushContent();
        packet.setType(PushContentType.STORY_ADDED);
        packet.setStory(StoryInfoUtil.build(
            story,
            true,
            t -> PostingInfoUtil.build(t.getEntry(), MediaAttachmentsProvider.NONE, AccessCheckers.ADMIN)
        ));
        return packet;
    }

    public static PushContent storyDeleted(UUID id) {
        PushContent packet = new PushContent();
        packet.setType(PushContentType.STORY_DELETED);
        packet.setId(id.toString());
        return packet;
    }

    public static PushContent feedUpdated(String feedName, FeedStatus feedStatus) {
        PushContent packet = new PushContent();
        packet.setType(PushContentType.FEED_UPDATED);
        packet.setFeedStatus(FeedWithStatusUtil.build(feedName, feedStatus));
        return packet;
    }

}
