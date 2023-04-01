package org.moera.node.rest;

import javax.inject.Inject;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.model.Result;
import org.moera.node.model.SheriffOrderAttributes;
import org.moera.node.rest.task.SheriffOrderPostTask;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/sheriff/orders")
@NoCache
public class RemoteSheriffOrderController {

    private static final Logger log = LoggerFactory.getLogger(RemoteSheriffOrderController.class);

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @PostMapping
    @Admin
    @Entitled
    public Result post(@PathVariable String nodeName,
                       @Valid @RequestBody SheriffOrderAttributes sheriffOrderAttributes) {
        log.info("POST /moera/api/nodes/{nodeName}/sheriff/orders (nodeName = {}, delete = {}, sheriffName = {},"
                        + " feedName = {}, postingId = {}, commentId = {}, category = {}, reasonCode = {})",
                LogUtil.format(nodeName),
                LogUtil.format(sheriffOrderAttributes.isDelete()),
                LogUtil.format(sheriffOrderAttributes.getSheriffName()),
                LogUtil.format(sheriffOrderAttributes.getFeedName()),
                LogUtil.format(sheriffOrderAttributes.getPostingId()),
                LogUtil.format(sheriffOrderAttributes.getCommentId()),
                LogUtil.format(sheriffOrderAttributes.getCategory().getValue()),
                LogUtil.format(sheriffOrderAttributes.getReasonCode().getValue()));

        var postTask = new SheriffOrderPostTask(nodeName, sheriffOrderAttributes);
        taskAutowire.autowire(postTask);
        taskExecutor.execute(postTask);

        return Result.OK;
    }

}
