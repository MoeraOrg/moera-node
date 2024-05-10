package org.moera.node.rest;

import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.SheriffOrder;
import org.moera.node.data.SheriffOrderRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.Result;
import org.moera.node.model.SheriffOrderAttributes;
import org.moera.node.model.SheriffOrderCategory;
import org.moera.node.model.SheriffOrderInfo;
import org.moera.node.model.SheriffOrderReason;
import org.moera.node.rest.task.SheriffOrderPostJob;
import org.moera.node.task.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
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
    private RequestContext requestContext;

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    @Inject
    private Jobs jobs;

    @PostMapping
    @Admin
    @Entitled
    public Result post(@PathVariable String nodeName,
                       @Valid @RequestBody SheriffOrderAttributes sheriffOrderAttributes) {
        log.info("POST /moera/api/nodes/{nodeName}/sheriff/orders (nodeName = {}, delete = {}, feedName = {},"
                        + " postingId = {}, commentId = {}, category = {}, reasonCode = {})",
                LogUtil.format(nodeName),
                LogUtil.format(sheriffOrderAttributes.isDelete()),
                LogUtil.format(sheriffOrderAttributes.getFeedName()),
                LogUtil.format(sheriffOrderAttributes.getPostingId()),
                LogUtil.format(sheriffOrderAttributes.getCommentId()),
                LogUtil.format(SheriffOrderCategory.toValue(sheriffOrderAttributes.getCategory())),
                LogUtil.format(SheriffOrderReason.toValue(sheriffOrderAttributes.getReasonCode())));

        if (sheriffOrderAttributes.getReasonCode() == null) {
            sheriffOrderAttributes.setReasonCode(SheriffOrderReason.OTHER);
        }

        jobs.run(
                SheriffOrderPostJob.class,
                new SheriffOrderPostJob.Parameters(nodeName, sheriffOrderAttributes, null),
                requestContext.nodeId());

        return Result.OK;
    }

    @GetMapping("/{id}")
    @Transactional
    public SheriffOrderInfo get(@PathVariable String nodeName, @PathVariable UUID id) {
        log.info("GET /moera/api/nodes/{nodeName}/sheriff/orders/{id} (nodeName = {}, id = {})",
                LogUtil.format(nodeName), LogUtil.format(id));

        SheriffOrder sheriffOrder = sheriffOrderRepository.findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("sheriff-order.not-found"));
        if (!Objects.equals(sheriffOrder.getRemoteNodeName(), nodeName)) {
            throw new ObjectNotFoundFailure("sheriff-order.wrong-node");
        }

        return new SheriffOrderInfo(sheriffOrder, requestContext.nodeName());
    }

}
