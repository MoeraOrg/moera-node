package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.AsyncOperationCreated;
import org.moera.lib.node.types.BlockedOperation;
import org.moera.lib.node.types.RemotePostingRepublishAttributes;
import org.moera.lib.node.types.PostingSourceText;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.data.FavorType;
import org.moera.node.data.OwnPosting;
import org.moera.node.data.OwnPostingRepository;
import org.moera.node.data.Pick;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.RemotePostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AsyncOperationCreatedUtil;
import org.moera.node.model.StoryAttributesUtil;
import org.moera.node.operations.BlockedUserOperations;
import org.moera.node.operations.FavorOperations;
import org.moera.node.picker.PickerPool;
import org.moera.node.rest.task.RemotePostingPostJob;
import org.moera.node.rest.task.verification.RemotePostingVerifyTask;
import org.moera.node.task.Jobs;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/postings")
@NoCache
public class RemotePostingController {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private OwnPostingRepository ownPostingRepository;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private FavorOperations favorOperations;

    @Inject
    private BlockedUserOperations blockedUserOperations;

    @Inject
    private PickerPool pickerPool;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private Jobs jobs;

    @PostMapping
    @Admin(Scope.REMOTE_ADD_POST)
    @Entitled
    @Transactional
    public Result post(@PathVariable String nodeName, @RequestBody PostingSourceText postingText) {
        log.info(
            "POST /nodes/{nodeName}/postings (nodeName = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(nodeName),
            LogUtil.format(postingText.getBodySrc(), 64),
            LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat()))
        );

        postingText.validate();
        ValidationUtil.assertion(
            postingText.getBodySrc() != null || !ObjectUtils.isEmpty(postingText.getMedia()),
            "posting.body-src.blank"
        );
        update(nodeName, null, postingText);

        return Result.OK;
    }

    @PutMapping("/{postingId}")
    @Admin(Scope.REMOTE_UPDATE_POST)
    @Entitled
    @Transactional
    public Result put(
        @PathVariable String nodeName, @PathVariable String postingId, @RequestBody PostingSourceText postingText
    ) {
        log.info(
            "PUT /nodes/{nodeName}/postings/{postingId}"
                + " (nodeName = {}, postingId = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(nodeName),
            LogUtil.format(postingId),
            LogUtil.format(postingText.getBodySrc(), 64),
            LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat()))
        );

        postingText.validate();
        update(nodeName, postingId, postingText);

        return Result.OK;
    }

    private void update(String nodeName, String postingId, PostingSourceText postingText) {
        if (postingText.getPublications() != null) {
            postingText.getPublications().forEach(StoryAttributesUtil::validate);
        }
        mediaOperations.validateAvatar(postingText.getOwnerAvatar());
        jobs.runAfterCommit(
            RemotePostingPostJob.class,
            new RemotePostingPostJob.Parameters(nodeName, postingId, postingText),
            requestContext.nodeId()
        );
    }

    @DeleteMapping("/{postingId}")
    @Admin(Scope.REMOTE_DELETE_CONTENT)
    @Transactional
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId) {
        log.info(
            "DELETE /nodes/{nodeName}/postings/{postingId} (nodeName = {}, postingId = {})",
            LogUtil.format(nodeName), LogUtil.format(postingId)
        );

        OwnPosting ownPosting = ownPostingRepository
            .findByRemotePostingId(requestContext.nodeId(), nodeName, postingId)
            .orElse(null);
        if (ownPosting != null && !ownPosting.hasParentMedia()) {
            favorOperations.asyncAddFavor(nodeName, FavorType.UNPOST);
            requestContext.send(new RemotePostingUpdatedLiberin(nodeName, postingId));
        }

        return Result.OK;
    }

    @PostMapping("/{id}/republish")
    @Entitled
    @Transactional
    public Result republish(
        @PathVariable String nodeName,
        @PathVariable String id,
        @RequestBody RemotePostingRepublishAttributes attributes
    ) {
        log.info(
            "POST /nodes/{nodeName}/postings/{id}/republish (nodeName = {}, id = {})",
            LogUtil.format(nodeName), LogUtil.format(id)
        );

        ValidationUtil.assertion(
            attributes.getPublications() == null || attributes.getPublications().stream().noneMatch(Objects::isNull),
            "posting.publications.missing"
        );
        attributes.validate();
        attributes.getPublications().forEach(StoryAttributesUtil::validate);
        if (
            !requestContext.isAdmin(Scope.ADD_POST)
            && (
                ObjectUtils.isEmpty(requestContext.getClientName(Scope.ADD_POST))
                || !requestContext.getOptions().getBool("posting.non-admin.allowed")
            )
        ) {
            throw new AuthenticationException();
        }
        if (!ObjectUtils.isEmpty(attributes.getPublications()) && !requestContext.isAdmin(Scope.UPDATE_FEEDS)) {
            throw new AuthenticationException();
        }
        if (blockedUserOperations.isBlocked(BlockedOperation.POSTING)) {
            throw new UserBlockedException();
        }

        for (var publication : attributes.getPublications()) {
            Pick pick = new Pick();
            pick.setRemoteNodeName(nodeName);
            pick.setRemoteFeedName(attributes.getRemoteFeedName());
            pick.setRemotePostingId(id);
            pick.setFeedName(publication.getFeedName());
            pick.setPublishAt(Util.toTimestamp(publication.getPublishAt()));
            pick.setPinned(Boolean.TRUE.equals(publication.getPinned()));
            pick.setViewed(Boolean.TRUE.equals(publication.getViewed()));
            pick.setRead(Boolean.TRUE.equals(publication.getRead()));
            pick.setSatisfied(Boolean.TRUE.equals(publication.getSatisfied()));
            if (publication.getExternalSourceUri() != null) {
                pick.setExternalSourceUri(publication.getExternalSourceUri().toArray(String[]::new));
            }
            pick.setPostingViewPrincipal(attributes.getPostingView());
            pick.setAllowMultiple(true);
            pickerPool.pick(pick);
        }

        return Result.OK;
    }

    @PostMapping("/{id}/verify")
    @Admin(Scope.OTHER)
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String id) {
        log.info(
            "POST /nodes/{name}/postings/{id}/verify, (name = {}, id = {})",
            LogUtil.format(nodeName), LogUtil.format(id)
        );

        return executeVerifyTask(nodeName, id, null);
    }

    @PostMapping("/{id}/revisions/{revisionId}/verify")
    @Admin(Scope.OTHER)
    @Transactional
    public AsyncOperationCreated verifyRevision(
        @PathVariable String nodeName, @PathVariable String id, @PathVariable String revisionId
    ) {
        log.info(
            "POST /nodes/{name}/postings/{id}/revisions/{revisionId}/verify, (name = {}, id = {}, revisionId = {})",
            LogUtil.format(nodeName), LogUtil.format(id), LogUtil.format(revisionId)
        );

        return executeVerifyTask(nodeName, id, revisionId);
    }

    private AsyncOperationCreated executeVerifyTask(String nodeName, String id, String revisionId) {
        RemotePostingVerification data = new RemotePostingVerification(
            requestContext.nodeId(), nodeName, id, revisionId
        );
        data.setDeadline(Timestamp.from(
            Instant.now().plus(
                requestContext.getOptions().getDuration("remote-posting-verification.lifetime").getDuration()
            )
        ));
        remotePostingVerificationRepository.saveAndFlush(data);

        RemotePostingVerifyTask task = new RemotePostingVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return AsyncOperationCreatedUtil.build(data.getId());
    }

}
