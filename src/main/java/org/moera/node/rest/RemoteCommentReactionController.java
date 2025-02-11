package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AsyncOperationCreated;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.Result;
import org.moera.node.rest.task.RemoteCommentReactionPostJob;
import org.moera.node.rest.task.verification.RemoteReactionVerifyTask;
import org.moera.node.task.Jobs;
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
@NoCache
public class RemoteCommentReactionController {

    private static final Logger log = LoggerFactory.getLogger(RemoteCommentReactionController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private RequestContext requestContext;

    @Inject
    private Jobs jobs;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @PostMapping
    @Admin(Scope.REMOTE_REACT)
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

        jobs.run(
                RemoteCommentReactionPostJob.class,
                new RemoteCommentReactionPostJob.Parameters(nodeName, postingId, commentId, attributes),
                requestContext.nodeId());

        return Result.OK;
    }

    @DeleteMapping
    @Admin(Scope.REMOTE_REACT)
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId,
                         @PathVariable String commentId) {
        log.info("DELETE /nodes/{nodeName}/postings/{postingId}/comments/{commentId}/reactions"
                        + " (nodeName = {}, postingId = {}, commentId = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                LogUtil.format(commentId));

        return Result.OK;
    }

    @PostMapping("/{ownerName}/verify")
    @Admin(Scope.OTHER)
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String postingId,
                                        @PathVariable String commentId, @PathVariable String ownerName) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/comments/{commentId}/reactions/{ownerName}/verify"
                        + " (nodeName = {}, postingId = {}, commentId = {}, ownerName = {})",
                LogUtil.format(nodeName), LogUtil.format(postingId), LogUtil.format(commentId),
                LogUtil.format(ownerName));

        RemoteReactionVerification data = new RemoteReactionVerification(
                requestContext.nodeId(), nodeName, postingId, commentId, ownerName);
        data.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("remote-reaction-verification.lifetime").getDuration())));
        remoteReactionVerificationRepository.saveAndFlush(data);

        RemoteReactionVerifyTask task = new RemoteReactionVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return new AsyncOperationCreated(data.getId());
    }

}
