package org.moera.node.liberin.receptor;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FeedSheriffDataUpdatedLiberin;
import org.moera.node.liberin.model.FeedStatusUpdatedLiberin;
import org.moera.node.model.FeedStatusUtil;
import org.moera.node.model.event.FeedSheriffDataUpdatedEvent;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoriesStatusUpdatedEvent;
import org.moera.node.push.PushContentBuilder;
import org.springframework.util.ObjectUtils;

@LiberinReceptor
public class FeedReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void statusUpdated(FeedStatusUpdatedLiberin liberin) {
        send(liberin, new FeedStatusUpdatedEvent(liberin.getFeedName(), liberin.getStatus(), true));
        if (!Feed.isAdmin(liberin.getFeedName())) {
            send(
                liberin,
                new FeedStatusUpdatedEvent(liberin.getFeedName(), FeedStatusUtil.notAdmin(liberin.getStatus()), false)
            );
        }
        if (liberin.getChange() != null) {
            send(liberin, new StoriesStatusUpdatedEvent(liberin.getFeedName(), liberin.getChange()));
            if (!ObjectUtils.isEmpty(liberin.getInstantsUpdated())) {
                if (liberin.getChange().getViewed()) {
                    liberin.getInstantsUpdated().stream()
                            .map(Story::getId)
                            .map(PushContentBuilder::storyDeleted)
                            .forEach(this::send);
                } else {
                    liberin.getInstantsUpdated().stream()
                            .map(PushContentBuilder::storyAdded)
                            .forEach(this::send);
                }
            }
        }
        send(PushContentBuilder.feedUpdated(liberin.getFeedName(), liberin.getStatus()));
    }

    @LiberinMapping
    public void sheriffDataUpdated(FeedSheriffDataUpdatedLiberin liberin) {
        send(
            liberin,
            new FeedSheriffDataUpdatedEvent(liberin.getFeedName(), liberin.getSheriffs(), liberin.getSheriffMarks())
        );
    }

}
