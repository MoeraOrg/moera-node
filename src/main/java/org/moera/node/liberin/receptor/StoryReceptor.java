package org.moera.node.liberin.receptor;

import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.liberin.LiberinMapping;
import org.moera.node.liberin.LiberinReceptor;
import org.moera.node.liberin.LiberinReceptorBase;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.push.PushContent;

@LiberinReceptor
public class StoryReceptor extends LiberinReceptorBase {

    @Inject
    private StoryOperations storyOperations;

    @LiberinMapping
    public void updated(StoryUpdatedLiberin liberin) {
        Story story = liberin.getStory();

        if (!Feed.isAdmin(story.getFeedName())) {
            send(liberin, new StoryUpdatedEvent(story, false));
        }
        send(liberin, new StoryUpdatedEvent(story, true));
        FeedStatus feedStatus = storyOperations.getFeedStatus(story.getFeedName(), true);
        send(liberin, new FeedStatusUpdatedEvent(story.getFeedName(), feedStatus, true));
        if (!Feed.isAdmin(story.getFeedName())) {
            send(liberin, new FeedStatusUpdatedEvent(story.getFeedName(), feedStatus.notAdmin(), false));
        }
        send(liberin, PushContent.feedUpdated(story.getFeedName(), feedStatus));
    }

}
