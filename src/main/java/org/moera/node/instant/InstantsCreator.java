package org.moera.node.instant;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.FeedStatusUpdatedLiberin;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.FeedStatus;
import org.moera.node.operations.StoryOperations;

public class InstantsCreator {

    @Inject
    protected UniversalContext universalContext;

    @Inject
    private StoryOperations storyOperations;

    protected UUID nodeId() {
        return universalContext.nodeId();
    }

    protected String nodeName() {
        return universalContext.nodeName();
    }

    protected void storyAdded(Story story) {
        universalContext.send(new StoryAddedLiberin(story));
    }

    protected void storyUpdated(Story story) {
        universalContext.send(new StoryUpdatedLiberin(story));
    }

    protected void storyAddedOrUpdated(Story story, boolean isAdded) {
        if (isAdded) {
            storyAdded(story);
        } else {
            storyUpdated(story);
        }
    }

    protected void storyDeleted(Story story) {
        universalContext.send(new StoryDeletedLiberin(story));
    }

    protected void feedUpdated() {
        FeedStatus feedStatus = storyOperations.getFeedStatus(Feed.INSTANT, true);
        universalContext.send(new FeedStatusUpdatedLiberin(Feed.INSTANT, feedStatus));
    }

    protected void updateMoment(Story story) {
        storyOperations.updateMoment(story, nodeId());
    }

}
