package org.moera.node.rest;

import java.sql.Timestamp;
import javax.inject.Inject;

import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.RequestContext;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class StoryOperations {

    private static final Timestamp PINNED_TIME = Util.toTimestamp(90000000000000L); // 9E+13

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    private final MomentFinder momentFinder = new MomentFinder();

    public void updateMoment(Story story) {
        story.setMoment(momentFinder.find(
                moment -> storyRepository.countMoments(requestContext.nodeId(), story.getFeedName(), moment) == 0,
                !story.isPinned() ? story.getPublishedAt() : PINNED_TIME));
    }

}
