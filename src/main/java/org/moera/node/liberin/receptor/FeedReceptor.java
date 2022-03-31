package org.moera.node.liberin.receptor;

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
        send(liberin, new StoriesStatusUpdatedEvent(liberin.getFeedName(), liberin.getChange()));
        send(liberin, PushContent.feedUpdated(liberin.getFeedName(), liberin.getStatus()));
    }

}
