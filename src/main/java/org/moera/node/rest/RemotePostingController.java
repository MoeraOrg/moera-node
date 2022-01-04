package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.data.SourceFormat;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AsyncOperationCreated;
import org.moera.node.model.PostingSourceText;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.rest.task.RemotePostingPostTask;
import org.moera.node.rest.task.RemotePostingVerifyTask;
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
@RequestMapping("/moera/api/nodes/{nodeName}/postings")
@NoCache
public class RemotePostingController {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private RequestContext requestContext;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @Inject
    private MediaOperations mediaOperations;

    @PostMapping
    @Admin
    @Entitled
    @Transactional
    public Result post(@PathVariable String nodeName, @Valid @RequestBody PostingSourceText postingText) {
        log.info("POST /nodes/{nodeName}/postings (nodeName = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        update(nodeName, null, postingText);

        return Result.OK;
    }

    @PutMapping("/{postingId}")
    @Admin
    @Entitled
    @Transactional
    public Result put(@PathVariable String nodeName, @PathVariable String postingId,
                      @Valid @RequestBody PostingSourceText postingText) {
        log.info("PUT /nodes/{nodeName}/postings/{postingId}"
                        + " (nodeName = {}, postingId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(postingText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat())));

        update(nodeName, postingId, postingText);

        return Result.OK;
    }

    private void update(String nodeName, String postingId, PostingSourceText postingText) {
        mediaOperations.validateAvatar(
                postingText.getOwnerAvatar(),
                postingText::setOwnerAvatarMediaFile,
                () -> new ValidationFailure("postingText.ownerAvatar.mediaId.not-found"));
        var postTask = new RemotePostingPostTask(nodeName, postingId, postingText);
        taskAutowire.autowire(postTask);
        taskExecutor.execute(postTask);
    }

    @DeleteMapping("/{postingId}")
    @Admin
    @Transactional
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId) {
        log.info("DELETE /nodes/{nodeName}/postings/{postingId} (nodeName = {}, postingId = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId));

        /*OwnComment ownComment = ownCommentRepository.findByRemoteCommentId(requestContext.nodeId(), nodeName,
                postingId, commentId).orElse(null);
        if (ownComment != null) {
            contactOperations.updateCloseness(nodeName, -1);
            contactOperations.updateCloseness(ownComment.getRemoteRepliedToName(), -1);
            requestContext.send(new RemoteCommentUpdatedEvent(nodeName, postingId, commentId));
        }*/

        return Result.OK;
    }

    @PostMapping("/{id}/verify")
    @Admin
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String id) {
        log.info("POST /nodes/{name}/postings/{id}/verify, (name = {}, id = {})",
                LogUtil.format(nodeName), LogUtil.format(id));

        return executeVerifyTask(nodeName, id, null);
    }

    @PostMapping("/{id}/revisions/{revisionId}/verify")
    @Admin
    @Transactional
    public AsyncOperationCreated verifyRevision(@PathVariable String nodeName, @PathVariable String id,
                                 @PathVariable String revisionId) {
        log.info("POST /nodes/{name}/postings/{id}/revisions/{revisionId}/verify, (name = {}, id = {}, revisionId = {})",
                LogUtil.format(nodeName), LogUtil.format(id), LogUtil.format(revisionId));

        return executeVerifyTask(nodeName, id, revisionId);
    }

    private AsyncOperationCreated executeVerifyTask(String nodeName, String id, String revisionId) {
        RemotePostingVerification data = new RemotePostingVerification(
                requestContext.nodeId(), nodeName, id, revisionId);
        data.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("remote-posting-verification.lifetime").getDuration())));
        remotePostingVerificationRepository.saveAndFlush(data);

        RemotePostingVerifyTask task = new RemotePostingVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return new AsyncOperationCreated(data.getId());
    }

}
