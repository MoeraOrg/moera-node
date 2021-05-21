package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.OwnComment;
import org.moera.node.data.OwnCommentRepository;
import org.moera.node.data.RemoteCommentVerification;
import org.moera.node.data.RemoteCommentVerificationRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.data.SubscriptionReason;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.RequestContext;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AsyncOperationCreated;
import org.moera.node.model.CommentSourceText;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.model.event.RemoteCommentUpdatedEvent;
import org.moera.node.operations.ContactOperations;
import org.moera.node.rest.task.RemoteCommentPostTask;
import org.moera.node.rest.task.RemoteCommentVerifyTask;
import org.moera.node.rest.task.RemotePostingCommentsSubscribeTask;
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
public class RemoteCommentController {

    private static Logger log = LoggerFactory.getLogger(RemoteCommentController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private RequestContext requestContext;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private RemoteCommentVerificationRepository remoteCommentVerificationRepository;

    @Inject
    private OwnCommentRepository ownCommentRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private MediaOperations mediaOperations;

    @PostMapping
    @Admin
    @Entitled
    public Result post(@PathVariable String nodeName, @PathVariable String postingId,
                       @Valid @RequestBody CommentSourceText commentText) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/comments"
                        + " (nodeName = {}, postingId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(commentText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat())));

        update(nodeName, postingId, null, commentText);

        return Result.OK;
    }

    @PutMapping("/{commentId}")
    @Admin
    @Entitled
    public Result put(@PathVariable String nodeName, @PathVariable String postingId, @PathVariable String commentId,
                      @Valid @RequestBody CommentSourceText commentText) {
        log.info("PUT /nodes/{nodeName}/postings/{postingId}/comments/{commentId}"
                        + " (nodeName = {}, postingId = {}, commentId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(commentId),
                LogUtil.format(commentText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(commentText.getBodySrcFormat())));

        update(nodeName, postingId, commentId, commentText);

        return Result.OK;
    }

    private void update(String nodeName, String postingId, String commentId, CommentSourceText commentText) {
        mediaOperations.validateAvatar(
                commentText.getOwnerAvatar(),
                commentText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("commentText.ownerAvatar.mediaId.not-found"));
        var postTask = new RemoteCommentPostTask(nodeName, postingId, commentId, commentText);
        taskAutowire.autowire(postTask);
        taskExecutor.execute(postTask);
        if (!nodeName.equals(requestContext.nodeName())) {
            var subscribeTask = new RemotePostingCommentsSubscribeTask(nodeName, postingId, SubscriptionReason.COMMENT);
            taskAutowire.autowire(subscribeTask);
            taskExecutor.execute(subscribeTask);
        }
    }

    @DeleteMapping("/{commentId}")
    @Admin
    @Transactional
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId, @PathVariable String commentId) {
        log.info("DELETE /nodes/{nodeName}/postings/{postingId}/comments/{commentId}"
                        + " (nodeName = {}, postingId = {}, commentId = {}",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(commentId));

        OwnComment ownComment = ownCommentRepository.findByRemoteCommentId(requestContext.nodeId(), nodeName,
                postingId, commentId).orElse(null);
        if (ownComment != null) {
            contactOperations.updateCloseness(nodeName, -1);
            contactOperations.updateCloseness(ownComment.getRemoteRepliedToName(), -1);
            requestContext.send(new RemoteCommentUpdatedEvent(nodeName, postingId, commentId));
        }

        return Result.OK;
    }

    @PostMapping("/{commentId}/verify")
    @Admin
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String postingId,
                                        @PathVariable String commentId) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/comments/{commentId}/verify"
                        + " (nodeName = {}, postingId = {}, commentId = {})",
                LogUtil.format(nodeName), LogUtil.format(postingId), LogUtil.format(commentId));

        RemoteCommentVerification data = new RemoteCommentVerification(
                requestContext.nodeId(), nodeName, postingId, commentId, null);
        data.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("remote-comment-verification.lifetime").getDuration())));
        remoteCommentVerificationRepository.saveAndFlush(data);

        RemoteCommentVerifyTask task = new RemoteCommentVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return new AsyncOperationCreated(data.getId());
    }

}
