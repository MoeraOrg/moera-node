package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.FeedStatus;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfo;
import org.moera.node.model.StoryAttributes;
import org.moera.node.model.StoryInfo;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.operations.StoryOperations;
import org.moera.node.push.PushContent;
import org.moera.node.push.PushService;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/stories")
@NoCache
public class StoryController {

    private static final Logger log = LoggerFactory.getLogger(StoryController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private PushService pushService;

    @GetMapping("/{id}")
    @Transactional
    public StoryInfo get(@PathVariable UUID id) {
        log.info("GET /stories/{id}, (id = {})", LogUtil.format(id));

        Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (story == null || Feed.isAdmin(story.getFeedName()) && !requestContext.isAdmin()) {
            throw new ObjectNotFoundFailure("story.not-found");
        }

        return StoryInfo.build(story, requestContext.isAdmin(), t -> new PostingInfo(t.getEntry().getId()));
    }

    @PutMapping("/{id}")
    @Admin
    @Transactional
    public StoryInfo put(@PathVariable UUID id, @Valid @RequestBody StoryAttributes storyAttributes) throws Throwable {
        log.info("PUT /stories/{id}, (id = {}, publishAt = {}, pinned = {}, viewed = {}, read = {})",
                LogUtil.format(id),
                LogUtil.formatTimestamp(storyAttributes.getPublishAt()),
                LogUtil.format(storyAttributes.getPinned()),
                LogUtil.format(storyAttributes.getViewed()),
                LogUtil.format(storyAttributes.getRead()));

        Story story = Transaction.execute(txManager, () -> {
            Story currentStory = storyRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                    .orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
            storyAttributes.toStory(currentStory);
            if (storyAttributes.getFeedName() != null
                    || storyAttributes.getPublishAt() != null
                    || storyAttributes.getPinned() != null) {
                storyOperations.updateMoment(currentStory);
            }
            return currentStory;
        });

        if (storyAttributes.getViewed() != null) {
            PushContent content;
            if (storyAttributes.getViewed()) {
                content = PushContent.storyDeleted(id);
            } else {
                content = PushContent.storyAdded(story);
            }
            pushService.send(requestContext.nodeId(), content);
        }

        if (!Feed.isAdmin(story.getFeedName())) {
            requestContext.send(new StoryUpdatedEvent(story, false));
        }
        requestContext.send(new StoryUpdatedEvent(story, true));
        FeedStatus feedStatus = storyOperations.getFeedStatus(story.getFeedName(), true);
        requestContext.send(new FeedStatusUpdatedEvent(story.getFeedName(), feedStatus, true));
        if (!Feed.isAdmin(story.getFeedName())) {
            requestContext.send(new FeedStatusUpdatedEvent(story.getFeedName(), feedStatus.notAdmin(), false));
        }
        pushService.send(requestContext.nodeId(), PushContent.feedUpdated(story.getFeedName(), feedStatus));

        return StoryInfo.build(story, requestContext.isAdmin(), t -> new PostingInfo(t.getEntry().getId()));
    }

}
