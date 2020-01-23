package org.moera.node.rest;

import java.security.PrivateKey;
import java.sql.Timestamp;
import java.time.Instant;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.RemoteReactionVerification;
import org.moera.node.data.RemoteReactionVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.AsyncOperationCreated;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.moera.node.rest.task.RemoteReactionPostTask;
import org.moera.node.rest.task.RemoteReactionVerifyTask;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/postings/{postingId}/reactions")
public class RemoteReactionController {

    private static Logger log = LoggerFactory.getLogger(RemoteReactionController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private RequestContext requestContext;

    @Inject
    private AutowireCapableBeanFactory autowireCapableBeanFactory;

    @Inject
    private RemoteReactionVerificationRepository remoteReactionVerificationRepository;

    @PostMapping
    public Result post(@PathVariable String nodeName, @PathVariable String postingId,
                       @Valid @RequestBody ReactionAttributes attributes) {
        log.info("POST /nodes/{nodeName}/postings/{postingId}/reactions"
                        + " (nodeName = {}, postingId = {}, negative = {}, emoji = {})",
                LogUtil.format(nodeName),
                LogUtil.format(postingId),
                attributes.isNegative() ? "yes" : "no",
                LogUtil.format(attributes.getEmoji()));

        Options options = requestContext.getOptions();
        String ownerName = options.nodeName();
        if (ownerName == null) {
            throw new OperationFailure("reaction.node-name-not-set");
        }
        PrivateKey signingKey = options.getPrivateKey("profile.signing-key");
        if (signingKey == null) {
            throw new OperationFailure("reaction.signing-key-not-set");
        }

        RemoteReactionPostTask task = new RemoteReactionPostTask(
                requestContext.nodeId(), nodeName, postingId, ownerName, signingKey, attributes);
        autowireCapableBeanFactory.autowireBean(task);
        taskExecutor.execute(task);

        return Result.OK;
    }

    @PostMapping("/{ownerName}/verify")
    @Admin
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String postingId,
                                        @PathVariable String ownerName) {
        log.info("POST /nodes/{name}/postings/{postingId}/reactions/{ownerName}/verify"
                        + " (name = {}, postingId = {}, ownerName = {})",
                LogUtil.format(nodeName), LogUtil.format(postingId), LogUtil.format(ownerName));

        RemoteReactionVerification data = new RemoteReactionVerification(
                requestContext.nodeId(), nodeName, postingId, ownerName);
        data.setDeadline(Timestamp.from(Instant.now().plus(
                requestContext.getOptions().getDuration("remote-reaction-verification.lifetime"))));
        remoteReactionVerificationRepository.saveAndFlush(data);

        RemoteReactionVerifyTask task = new RemoteReactionVerifyTask(data);
        autowireCapableBeanFactory.autowireBean(task);
        taskExecutor.execute(task);

        return new AsyncOperationCreated(data.getId());
    }

    @Scheduled(fixedDelayString = "PT30M")
    @Transactional
    public void purgeExpiredVerifications() {
        remoteReactionVerificationRepository.deleteExpired(Util.now());
    }

}
