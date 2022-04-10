package org.moera.node.liberin.receptor;

import org.moera.node.data.Feed;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.FeedStatusUpdatedLiberin;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoriesStatusUpdatedEvent;
import org.moera.node.push.PushContent;

@LiberinReceptor
public class FeedReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void statusUpdated(FeedStatusUpdatedLiberin liberin) {
        send(liberin, new FeedStatusUpdatedEvent(liberin.getFeedName(), liberin.getStatus(), true));
        if (!Feed.isAdmin(liberin.getFeedName())) {
            send(liberin,
                    new FeedStatusUpdatedEvent(liberin.getFeedName(), liberin.getStatus().notAdmin(), false));
        }
        if (liberin.getChange() != null) {
            send(liberin, new StoriesStatusUpdatedEvent(liberin.getFeedName(), liberin.getChange()));
        }
        send(PushContent.feedUpdated(liberin.getFeedName(), liberin.getStatus()));
    }

}
