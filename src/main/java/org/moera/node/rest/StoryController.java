package org.moera.node.rest;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.node.types.StoryType;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.media.DirectServeOperations;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.StoryAttributesUtil;
import org.moera.node.model.StoryInfoUtil;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
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
    private DirectServeOperations directServeOperations;

    @Inject
    private RequestContext requestContext;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private EntryOperations entryOperations;

    @Inject
    private Transaction tx;

    @GetMapping("/{id}")
    @Transactional
    public StoryInfo get(@PathVariable String id) {
        log.info("GET /stories/{id} (id = {})", LogUtil.format(id));

        UUID storyId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
        Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), storyId).orElse(null);
        if (story == null || Feed.isAdmin(story.getFeedName()) && !requestContext.isAdmin(Scope.VIEW_FEEDS)) {
            throw new ObjectNotFoundFailure("story.not-found");
        }
        if (
            story.getEntry() != null
            && !requestContext.isPrincipal(story.getViewPrincipalFilter(), Scope.VIEW_CONTENT)
        ) {
            throw new ObjectNotFoundFailure("story.not-found");
        }

        return StoryInfoUtil.build(
            story,
            requestContext.isAdmin(Scope.VIEW_FEEDS),
            t -> PostingInfoUtil.build(
                t.getEntry(),
                entryOperations,
                requestContext,
                requestContext.getOptions(),
                directServeOperations
            ),
            directServeOperations
        );
    }

    @PutMapping("/{id}")
    @Admin(Scope.UPDATE_FEEDS)
    public StoryInfo put(@PathVariable String id, @RequestBody StoryAttributes storyAttributes) {
        log.info(
            "PUT /stories/{id} (id = {}, publishAt = {}, pinned = {}, viewed = {}, read = {})",
            LogUtil.format(id),
            LogUtil.formatTimestamp(storyAttributes.getPublishAt()),
            LogUtil.format(storyAttributes.getPinned()),
            LogUtil.format(storyAttributes.getViewed()),
            LogUtil.format(storyAttributes.getRead())
        );

        UUID storyId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
        StoryInfo storyInfo = tx.executeWrite(() -> {
            Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), storyId)
                .orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
            if (
                story.getEntry() != null
                && !requestContext.isPrincipal(story.getViewPrincipalFilter(), Scope.VIEW_CONTENT)
            ) {
                throw new ObjectNotFoundFailure("story.not-found");
            }
            boolean wasShared = story.getStoryType() == StoryType.POSTING_ADDED
                && Feed.TIMELINE.equals(story.getFeedName()) && story.getEntry() != null
                && !story.getEntry().isOriginal();
            StoryDeletedLiberin previous = wasShared ? new StoryDeletedLiberin(story) : null;
            StoryAttributesUtil.toStory(storyAttributes, story);
            if (
                storyAttributes.getFeedName() != null
                || storyAttributes.getPublishAt() != null
                || storyAttributes.getPinned() != null
            ) {
                storyOperations.updateMoment(story, requestContext.nodeId());
            }

            boolean isShared = story.getStoryType() == StoryType.POSTING_ADDED
                && Feed.TIMELINE.equals(story.getFeedName()) && story.getEntry() != null
                && !story.getEntry().isOriginal();
            if (wasShared && !isShared) {
                requestContext.send(previous);
            } else if (!wasShared && isShared) {
                requestContext.send(new StoryAddedLiberin(story));
            }
            return StoryInfoUtil.build(
                story,
                requestContext.isAdmin(Scope.VIEW_FEEDS),
                t -> PostingInfoUtil.build(
                    t.getEntry(),
                    entryOperations,
                    requestContext,
                    requestContext.getOptions(),
                    directServeOperations
                ),
                directServeOperations
            );
        });

        requestContext.send(new StoryUpdatedLiberin(storyId));

        return storyInfo;
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.UPDATE_FEEDS)
    public Result delete(@PathVariable String id) {
        log.info("DELETE /stories/{id} (id = {})", LogUtil.format(id));

        UUID storyId = Util.uuid(id).orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
        var liberin = tx.executeWrite(() -> {
            Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), storyId)
                .orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
            storyRepository.delete(story);

            return new StoryDeletedLiberin(story);
        });

        requestContext.send(liberin);

        return Result.OK;
    }

}
