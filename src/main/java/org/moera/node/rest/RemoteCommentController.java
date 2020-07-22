package org.moera.node.rest;

import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.SourceFormat;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.RequestContext;
import org.moera.node.model.CommentSourceText;
import org.moera.node.model.Result;
import org.moera.node.rest.task.RemoteCommentPostTask;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
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

    /* TODO @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;*/

    @PostMapping
    @Admin
    @Entitled
    public Result post(@PathVariable String nodeName, @PathVariable String postingId,
                       @Valid @RequestBody CommentSourceText sourceText) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/comments"
                        + " (nodeName = {}, postingId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(sourceText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(sourceText.getBodySrcFormat())));

        RemoteCommentPostTask task = new RemoteCommentPostTask(nodeName, postingId, null, sourceText);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return Result.OK;
    }

    @PutMapping("/{commentId}")
    @Admin
    @Entitled
    public Result put(@PathVariable String nodeName, @PathVariable String postingId, @PathVariable String commentId,
                      @Valid @RequestBody CommentSourceText sourceText) {
        log.info("PUT /nodes/{nodeName}/postings/{postingId}/comments/{commentId}"
                        + " (nodeName = {}, postingId = {}, commentId = {}, bodySrc = {}, bodySrcFormat = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(commentId),
                LogUtil.format(sourceText.getBodySrc(), 64),
                LogUtil.format(SourceFormat.toValue(sourceText.getBodySrcFormat())));

        RemoteCommentPostTask task = new RemoteCommentPostTask(nodeName, postingId, commentId, sourceText);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return Result.OK;
    }

    /* TODO @DeleteMapping
    @Admin
    @Transactional
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId) {
        log.info("DELETE /nodes/{nodeName}/postings/{postingId}/reactions (nodeName = {}, postingId = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId));

        requestContext.send(new RemoteReactionDeletedEvent(nodeName, postingId));

        return Result.OK;
    }

    @PostMapping("/{ownerName}/verify")
    @Admin
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String postingId,
                                        @PathVariable String ownerName) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/reactions/{ownerName}/verify"
                        + " (nodeName = {}, postingId = {}, ownerName = {})",
                LogUtil.format(nodeName), LogUtil.format(postingId), LogUtil.format(ownerName));

        RemoteReactionVerification data = new RemoteReactionVerification(
                requestContext.nodeId(), nodeName, postingId, ownerName);
        data.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("remote-reaction-verification.lifetime"))));
        remoteReactionVerificationRepository.saveAndFlush(data);

        RemoteReactionVerifyTask task = new RemoteReactionVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return new AsyncOperationCreated(data.getId());
    }

    @Scheduled(fixedDelayString = "PT30M")
    @Transactional
    public void purgeExpiredVerifications() {
        remoteReactionVerificationRepository.deleteExpired(Util.now());
    }*/

}
