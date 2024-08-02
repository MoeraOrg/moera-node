package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.auth.AuthScope;
import org.moera.node.auth.Scope;
import org.moera.node.data.OwnReaction;
import org.moera.node.data.OwnReactionRepository;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.RemotePostingReactionDeletedLiberin;
import org.moera.node.model.AsyncOperationCreated;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.Result;
import org.moera.node.operations.ContactOperations;
import org.moera.node.rest.task.RemotePostingReactionPostJob;
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
@RequestMapping("/moera/api/nodes/{nodeName}/postings/{postingId}/reactions")
@NoCache
public class RemotePostingReactionController {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingReactionController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @Inject
    private OwnReactionRepository ownReactionRepository;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private Jobs jobs;

    @PostMapping
    @Admin
    @AuthScope(Scope.REMOTE_REACT)
    @Entitled
    public Result post(@PathVariable String nodeName, @PathVariable String postingId,
                       @Valid @RequestBody ReactionAttributes attributes) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/reactions"
                        + " (nodeName = {}, postingId = {}, negative = {}, emoji = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                attributes.isNegative() ? "yes" : "no",
                LogUtil.format(attributes.getEmoji()));

        jobs.run(
                RemotePostingReactionPostJob.class,
                new RemotePostingReactionPostJob.Parameters(nodeName, postingId, attributes),
                requestContext.nodeId());

        return Result.OK;
    }

    @DeleteMapping
    @Admin
    @AuthScope(Scope.REMOTE_REACT)
    @Transactional
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId) {
        log.info("DELETE /nodes/{nodeName}/postings/{postingId}/reactions (nodeName = {}, postingId = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId));

        OwnReaction ownReaction = ownReactionRepository.findByRemotePostingId(requestContext.nodeId(), nodeName,
                postingId).orElse(null);
        if (ownReaction != null) {
            ownReactionRepository.delete(ownReaction);
            contactOperations.asyncUpdateCloseness(nodeName, -0.25f);
            requestContext.send(new RemotePostingReactionDeletedLiberin(nodeName, postingId));
        }

        return Result.OK;
    }

    @PostMapping("/{ownerName}/verify")
    @Admin
    @AuthScope(Scope.OTHER)
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String postingId,
                                        @PathVariable String ownerName) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/reactions/{ownerName}/verify"
                        + " (nodeName = {}, postingId = {}, ownerName = {})",
                LogUtil.format(nodeName), LogUtil.format(postingId), LogUtil.format(ownerName));

        RemoteReactionVerification data = new RemoteReactionVerification(
                requestContext.nodeId(), nodeName, postingId, ownerName);
        data.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("remote-reaction-verification.lifetime").getDuration())));
        remoteReactionVerificationRepository.saveAndFlush(data);

        RemoteReactionVerifyTask task = new RemoteReactionVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return new AsyncOperationCreated(data.getId());
    }

}
