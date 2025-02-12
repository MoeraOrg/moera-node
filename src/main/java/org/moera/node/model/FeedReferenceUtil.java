package org.moera.node.model;

import org.moera.lib.node.types.FeedReference;
import org.moera.lib.node.types.StoryOperations;
import org.moera.lib.node.types.principal.Principal;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

public class FeedReferenceUtil {

    public static FeedReference build(Story story) {
        FeedReference feedReference = new FeedReference();
        feedReference.setFeedName(story.getFeedName());
        feedReference.setPublishedAt(Util.toEpochSecond(story.getPublishedAt()));
        feedReference.setPinned(story.isPinned());
        feedReference.setMoment(story.getMoment());
        feedReference.setStoryId(story.getId().toString());

        StoryOperations operations = new StoryOperations();
        operations.setEdit(Principal.ADMIN);
        operations.setDelete(Principal.ADMIN);
        feedReference.setOperations(operations);

        return feedReference;
    }

}
