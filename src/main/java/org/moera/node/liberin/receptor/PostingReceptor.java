package org.moera.node.liberin.receptor;

import org.moera.node.data.Posting;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.PostingRestoredLiberin;
import org.moera.node.model.event.PostingRestoredEvent;
import org.moera.node.model.notification.PostingUpdatedNotification;
import org.moera.node.notification.send.Directions;

@LiberinReceptor
public class PostingReceptor extends LiberinReceptorBase {

    @LiberinMapping
    public void restored(PostingRestoredLiberin liberin) {
        Posting posting = liberin.getPosting();

        send(liberin, new PostingRestoredEvent(posting));
        send(Directions.postingSubscribers(posting.getNodeId(), posting.getId()),
                new PostingUpdatedNotification(posting.getId()));
    }

}
