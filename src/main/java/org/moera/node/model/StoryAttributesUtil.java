package org.moera.node.model;

import org.moera.lib.node.types.StoryAttributes;
import org.moera.node.data.Story;
import org.moera.node.util.Util;

public class StoryAttributesUtil {

    public static void toStory(StoryAttributes attributes, Story story) {
        if (attributes.getFeedName() != null) {
            story.setFeedName(attributes.getFeedName());
        }
        if (attributes.getPublishAt() != null) {
            story.setPublishedAt(Util.toTimestamp(attributes.getPublishAt()));
        }
        if (attributes.getPinned() != null) {
            story.setPinned(attributes.getPinned());
        }
        if (attributes.getViewed() != null) {
            story.setViewed(attributes.getViewed());
        }
        if (attributes.getRead() != null) {
            story.setRead(attributes.getRead());
        }
        if (attributes.getSatisfied() != null) {
            story.setSatisfied(attributes.getSatisfied());
        }
    }

}
