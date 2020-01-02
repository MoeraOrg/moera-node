package org.moera.node.rest;

import java.security.PrivateKey;
import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.ReactionAttributes;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.core.task.TaskExecutor;
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

}
