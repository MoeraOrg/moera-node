package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.AsyncOperationCreated;
import org.moera.lib.node.types.CommentSourceText;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.node.types.SubscriptionReason;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.OwnComment;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.data.RemoteCommentVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.RemoteCommentUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AsyncOperationCreatedUtil;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.SubscriptionOperations;
import org.moera.node.rest.task.RemoteCommentPostJob;
import org.moera.node.rest.task.verification.RemoteCommentVerifyTask;
import org.moera.node.task.Jobs;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/postings/{postingId}/comments")
@NoCache
public class RemoteCommentController {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private RemoteCommentVerificationRepository remoteCommentVerificationRepository;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private SubscriptionOperations subscriptionOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private Jobs jobs;

    @PostMapping
    @Admin(Scope.REMOTE_ADD_COMMENT)
    @Entitled
    @Transactional
    public Result post(
        @PathVariable String nodeName,
        @PathVariable String postingId,
        @RequestBody CommentSourceText commentText
    ) {
        log.info(
            "POST /nodes/{nodeName}/postings/{postingId}/comments"
                + " (nodeName = {}, postingId = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(nodeName),
            LogUtil.format(postingId),
            LogUtil.format(commentText.getBodySrc(), 64),
            LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat()))
        );

        commentText.validate();
        update(nodeName, postingId, null, commentText);

        return Result.OK;
    }

    @PutMapping("/{commentId}")
    @Admin(Scope.REMOTE_UPDATE_COMMENT)
    @Entitled
    @Transactional
    public Result put(
        @PathVariable String nodeName,
        @PathVariable String postingId,
        @PathVariable String commentId,
        @RequestBody CommentSourceText commentText
    ) {
        log.info(
            "PUT /nodes/{nodeName}/postings/{postingId}/comments/{commentId}"
                + " (nodeName = {}, postingId = {}, commentId = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(nodeName),
            LogUtil.format(postingId),
            LogUtil.format(commentId),
            LogUtil.format(commentText.getBodySrc(), 64),
            LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat()))
        );

        commentText.validate();
        update(nodeName, postingId, commentId, commentText);

        return Result.OK;
    }

    private void update(String nodeName, String postingId, String commentId, CommentSourceText commentText) {
        mediaOperations.validateAvatar(
            commentText.getOwnerAvatar(),
            mf -> AvatarDescriptionUtil.setMediaFile(commentText.getOwnerAvatar(), mf),
            () -> new ObjectNotFoundFailure("avatar.not-found")
        );
        jobs.run(
            RemoteCommentPostJob.class,
            new RemoteCommentPostJob.Parameters(nodeName, postingId, commentId, commentText),
            requestContext.nodeId()
        );
        if (!nodeName.equals(requestContext.nodeName())) {
            subscriptionOperations.subscribeToPostingComments(nodeName, postingId, SubscriptionReason.COMMENT);
        }
    }

    @DeleteMapping("/{commentId}")
    @Admin(Scope.REMOTE_DELETE_CONTENT)
    @Transactional
    public Result delete(
        @PathVariable String nodeName,
        @PathVariable String postingId,
        @PathVariable String commentId
    ) {
        log.info(
            "DELETE /nodes/{nodeName}/postings/{postingId}/comments/{commentId}"
                + " (nodeName = {}, postingId = {}, commentId = {}",
            LogUtil.format(nodeName), LogUtil.format(postingId), LogUtil.format(commentId)
        );

        OwnComment ownComment = ownCommentRepository.findByRemoteCommentId(
            requestContext.nodeId(), nodeName, postingId, commentId
        ).orElse(null);
        if (ownComment != null) {
            contactOperations.asyncUpdateCloseness(nodeName, -1);
            contactOperations.asyncUpdateCloseness(ownComment.getRemoteRepliedToName(), -1);
            requestContext.send(new RemoteCommentUpdatedLiberin(nodeName, postingId, commentId));
        }

        return Result.OK;
    }

    @PostMapping("/{commentId}/verify")
    @Admin(Scope.OTHER)
    @Transactional
    public AsyncOperationCreated verify(
        @PathVariable String nodeName,
        @PathVariable String postingId,
        @PathVariable String commentId
    ) {
        log.info(
            "POST /nodes/{nodeName}/postings/{postingId}/comments/{commentId}/verify"
                + " (nodeName = {}, postingId = {}, commentId = {})",
                LogUtil.format(nodeName), LogUtil.format(postingId), LogUtil.format(commentId)
        );

        RemoteCommentVerification data = new RemoteCommentVerification(
            requestContext.nodeId(), nodeName, postingId, commentId, null
        );
        data.setDeadline(Timestamp.from(
            Instant
                .now()
                .plus(requestContext.getOptions().getDuration("remote-comment-verification.lifetime").getDuration())
        ));
        remoteCommentVerificationRepository.saveAndFlush(data);

        RemoteCommentVerifyTask task = new RemoteCommentVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return AsyncOperationCreatedUtil.build(data.getId());
    }

}
