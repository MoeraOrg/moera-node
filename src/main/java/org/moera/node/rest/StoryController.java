package org.moera.node.rest;

import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.StoryAttributes;
import org.moera.lib.node.types.StoryInfo;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.PostingInfoUtil;
import org.moera.node.model.StoryAttributesUtil;
import org.moera.node.model.StoryInfoUtil;
import org.moera.node.operations.EntryOperations;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.util.Pair;
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
    public StoryInfo get(@PathVariable UUID id) {
        log.info("GET /stories/{id} (id = {})", LogUtil.format(id));

        Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
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
            t -> PostingInfoUtil.build(t.getEntry(), entryOperations, requestContext)
        );
    }

    @PutMapping("/{id}")
    @Admin(Scope.UPDATE_FEEDS)
    public StoryInfo put(@PathVariable UUID id, @RequestBody StoryAttributes storyAttributes) {
        log.info(
            "PUT /stories/{id} (id = {}, publishAt = {}, pinned = {}, viewed = {}, read = {})",
            LogUtil.format(id),
            LogUtil.formatTimestamp(storyAttributes.getPublishAt()),
            LogUtil.format(storyAttributes.getPinned()),
            LogUtil.format(storyAttributes.getViewed()),
            LogUtil.format(storyAttributes.getRead())
        );

        Pair<Story, StoryInfo> info = tx.executeWrite(() -> {
            Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
            if (
                story.getEntry() != null
                && !requestContext.isPrincipal(story.getViewPrincipalFilter(), Scope.VIEW_CONTENT)
            ) {
                throw new ObjectNotFoundFailure("story.not-found");
            }
            StoryAttributesUtil.toStory(storyAttributes, story);
            if (
                storyAttributes.getFeedName() != null
                || storyAttributes.getPublishAt() != null
                || storyAttributes.getPinned() != null
            ) {
                storyOperations.updateMoment(story, requestContext.nodeId());
            }

            StoryInfo storyInfo = StoryInfoUtil.build(
                story,
                requestContext.isAdmin(Scope.VIEW_FEEDS),
                t -> PostingInfoUtil.build(t.getEntry(), entryOperations, requestContext)
            );

            return Pair.of(story, storyInfo);
        });

        requestContext.send(new StoryUpdatedLiberin(info.getFirst()));

        return info.getSecond();
    }

    @DeleteMapping("/{id}")
    @Admin(Scope.UPDATE_FEEDS)
    public Result delete(@PathVariable UUID id) {
        log.info("DELETE /stories/{id} (id = {})", LogUtil.format(id));

        Story deletedStory = tx.executeWrite(() -> {
            Story story = storyRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("story.not-found"));
            storyRepository.delete(story);

            return story;
        });

        requestContext.send(new StoryDeletedLiberin(deletedStory));

        return Result.OK;
    }

}
