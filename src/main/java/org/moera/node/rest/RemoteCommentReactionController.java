package org.moera.node.rest;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.Result;
import org.moera.node.rest.task.RemoteCommentReactionPostTask;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/postings/{postingId}/comments/{commentId}/reactions")
public class RemoteCommentReactionController {

    private static Logger log = LoggerFactory.getLogger(RemoteCommentReactionController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private RequestContext requestContext;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @PostMapping
    @Admin
    @Entitled
    public Result post(@PathVariable String nodeName, @PathVariable String postingId, @PathVariable String commentId,
                       @Valid @RequestBody ReactionAttributes attributes) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/comments/{commentId}/reactions"
                        + " (nodeName = {}, postingId = {}, commentId = {}, negative = {}, emoji = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(commentId),
                attributes.isNegative() ? "yes" : "no",
                LogUtil.format(attributes.getEmoji()));

        var task = new RemoteCommentReactionPostTask(nodeName, postingId, commentId, attributes);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return Result.OK;
    }

    @DeleteMapping
    @Admin
    @Transactional
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId,
                         @PathVariable String commentId) {
        log.info("DELETE /nodes/{nodeName}/postings/{postingId}/comments/{commentId}/reactions"
                        + " (nodeName = {}, postingId = {}, commentId = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(commentId));

        return Result.OK;
    }

    /* TODO @PostMapping("/{ownerName}/verify")
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
