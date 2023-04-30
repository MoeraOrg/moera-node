package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.SheriffComplain;
import org.moera.node.data.SheriffComplainRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Result;
import org.moera.node.model.SheriffComplainText;
import org.moera.node.model.SheriffOrderReason;
import org.moera.node.rest.task.SheriffComplainPrepareTask;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/sheriff/complains")
@NoCache
public class SheriffComplainController {

    private static final Logger log = LoggerFactory.getLogger(SheriffComplainController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SheriffComplainRepository sheriffComplainRepository;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @PostMapping
    @Transactional
    public Result post(@Valid @RequestBody SheriffComplainText sheriffComplainText) {
        log.info("POST /sheriff/complains"
                        + " (nodeName = {}, feedName = {}, postingId = {}, commentId = {}, reasonCode = {})",
                LogUtil.format(sheriffComplainText.getNodeName()),
                LogUtil.format(sheriffComplainText.getFeedName()),
                LogUtil.format(sheriffComplainText.getPostingId()),
                LogUtil.format(sheriffComplainText.getCommentId()),
                LogUtil.format(SheriffOrderReason.toValue(sheriffComplainText.getReasonCode())));

        SheriffComplain sheriffComplain = new SheriffComplain();
        sheriffComplain.setId(UUID.randomUUID());
        sheriffComplain.setNodeId(requestContext.nodeId());
        sheriffComplain.setOwnerName(requestContext.getClientName());
        sheriffComplainText.toSheriffComplain(sheriffComplain);
        sheriffComplainRepository.save(sheriffComplain);

        var prepareTask = new SheriffComplainPrepareTask(sheriffComplain.getId(), sheriffComplain.getRemoteNodeName(),
                sheriffComplain.getRemoteFeedName(), sheriffComplain.getRemotePostingId(),
                sheriffComplain.getRemoteCommentId());
        taskAutowire.autowire(prepareTask);
        taskExecutor.execute(prepareTask);

        return Result.OK; // FIXME return SheriffComplainInfo
    }

}
