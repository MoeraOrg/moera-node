package org.moera.node.rest;

import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.SheriffOrdersSliceInfo;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.SheriffOrder;
import org.moera.node.data.SheriffOrderRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.SheriffOrderInfoUtil;
import org.moera.node.util.SafeInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/activity/sheriff/orders")
@NoCache
public class ActivitySheriffOrderController {

    private static final Logger log = LoggerFactory.getLogger(ActivitySheriffOrderController.class);

    private static final int MAX_ORDERS_PER_REQUEST = 200;

    @Inject
    private RequestContext requestContext;

    @Inject
    private SheriffOrderRepository sheriffOrderRepository;

    @GetMapping
    @Transactional
    public SheriffOrdersSliceInfo getOrders(
        @RequestParam(required = false) Long before,
        @RequestParam(required = false) Long after,
        @RequestParam(required = false) Integer limit
    ) {
        log.info(
            "GET /activity/sheriff/orders (before = {}, after = {}, limit = {})",
            LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit)
        );

        ValidationUtil.assertion(before == null || after == null, "sheriff-orders.before-after-exclusive");

        limit = limit != null && limit <= MAX_ORDERS_PER_REQUEST ? limit : MAX_ORDERS_PER_REQUEST;
        ValidationUtil.minValue(limit, 0, "limit.invalid");

        SheriffOrdersSliceInfo sliceInfo;
        if (after == null) {
            before = before != null ? before : SafeInteger.MAX_VALUE;
            sliceInfo = getItemsBefore(before, limit);
        } else {
            sliceInfo = getItemsAfter(after, limit);
        }
        calcSliceTotals(sliceInfo);

        return sliceInfo;
    }

    private SheriffOrdersSliceInfo getItemsBefore(long before, int limit) {
        SheriffOrdersSliceInfo sliceInfo = new SheriffOrdersSliceInfo();
        sliceInfo.setBefore(before);
        Page<SheriffOrder> page = findSlice(
            requestContext.nodeId(), SafeInteger.MIN_VALUE, before, limit + 1, Sort.Direction.DESC
        );
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private SheriffOrdersSliceInfo getItemsAfter(long after, int limit) {
        SheriffOrdersSliceInfo sliceInfo = new SheriffOrdersSliceInfo();
        sliceInfo.setAfter(after);
        Page<SheriffOrder> page = findSlice(
            requestContext.nodeId(), after, SafeInteger.MAX_VALUE, limit + 1, Sort.Direction.ASC
        );
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(SafeInteger.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private Page<SheriffOrder> findSlice(
        UUID nodeId, long afterMoment, long beforeMoment, int limit, Sort.Direction direction
    ) {
        Pageable pageable = PageRequest.of(0, limit + 1, direction, "moment");
        return sheriffOrderRepository.findInRange(nodeId, afterMoment, beforeMoment, pageable);
    }

    private void fillSlice(SheriffOrdersSliceInfo sliceInfo, Page<SheriffOrder> page) {
        sliceInfo.setOrders(
            page.getContent().stream()
                .map(order -> SheriffOrderInfoUtil.build(order, requestContext.nodeName()))
                .collect(Collectors.toList())
        );
    }

    private void calcSliceTotals(SheriffOrdersSliceInfo sliceInfo) {
        int total = sheriffOrderRepository.countByNodeId(requestContext.nodeId());
        sliceInfo.setTotal(total);
        if (sliceInfo.getAfter() <= SafeInteger.MIN_VALUE) {
            sliceInfo.setTotalInPast(0);
            sliceInfo.setTotalInFuture(total - sliceInfo.getOrders().size());
        } else if (sliceInfo.getBefore() >= SafeInteger.MAX_VALUE) {
            sliceInfo.setTotalInFuture(0);
            sliceInfo.setTotalInPast(total - sliceInfo.getOrders().size());
        } else {
            int totalInFuture = sheriffOrderRepository.countInRange(
                requestContext.nodeId(), sliceInfo.getBefore(), SafeInteger.MAX_VALUE
            );
            sliceInfo.setTotalInFuture(totalInFuture);
            sliceInfo.setTotalInPast(total - totalInFuture - sliceInfo.getOrders().size());
        }
    }

}
