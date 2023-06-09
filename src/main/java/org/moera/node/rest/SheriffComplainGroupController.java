package org.moera.node.rest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.SheriffComplain;
import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainGroupRepository;
import org.moera.node.data.SheriffComplainRepository;
import org.moera.node.data.SheriffComplainStatus;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SheriffComplainGroupUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.SheriffComplainDecisionText;
import org.moera.node.model.SheriffComplainGroupInfo;
import org.moera.node.model.SheriffComplainGroupsSliceInfo;
import org.moera.node.model.SheriffComplainInfo;
import org.moera.node.model.SheriffOrderAttributes;
import org.moera.node.model.SheriffOrderCategory;
import org.moera.node.model.ValidationFailure;
import org.moera.node.rest.task.SheriffOrderPostTask;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.SafeInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/sheriff/complains/groups")
@NoCache
public class SheriffComplainGroupController {

    private static final Logger log = LoggerFactory.getLogger(SheriffComplainGroupController.class);

    private static final int MAX_GROUPS_PER_REQUEST = 200;

    @Inject
    private RequestContext requestContext;

    @Inject
    private SheriffComplainGroupRepository sheriffComplainGroupRepository;

    @Inject
    private SheriffComplainRepository sheriffComplainRepository;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @GetMapping
    @Transactional
    public SheriffComplainGroupsSliceInfo getAll(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) SheriffComplainStatus status) {

        log.info("GET /sheriff/complains/groups (before = {}, after = {}, limit = {}, status = {})",
                LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit),
                LogUtil.format(Objects.toString(status)));

        if (before != null && after != null) {
            throw new ValidationFailure("sheriff-complain-groups.before-after-exclusive");
        }

        limit = limit != null && limit <= MAX_GROUPS_PER_REQUEST ? limit : MAX_GROUPS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        SheriffComplainGroupsSliceInfo sliceInfo;
        if (after == null) {
            before = before != null ? before : SafeInteger.MAX_VALUE;
            sliceInfo = getGroupsBefore(before, limit, status);
        } else {
            sliceInfo = getGroupsAfter(after, limit, status);
        }
        calcSliceTotals(sliceInfo, status);

        return sliceInfo;
    }

    private SheriffComplainGroupsSliceInfo getGroupsBefore(long before, int limit, SheriffComplainStatus status) {
        SheriffComplainGroupsSliceInfo sliceInfo = new SheriffComplainGroupsSliceInfo();
        sliceInfo.setBefore(before);
        Page<SheriffComplainGroup> page = findSlice(requestContext.nodeId(), SafeInteger.MIN_VALUE, before,
                limit + 1, Sort.Direction.DESC, status);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private SheriffComplainGroupsSliceInfo getGroupsAfter(long after, int limit, SheriffComplainStatus status) {
        SheriffComplainGroupsSliceInfo sliceInfo = new SheriffComplainGroupsSliceInfo();
        sliceInfo.setAfter(after);
        Page<SheriffComplainGroup> page = findSlice(requestContext.nodeId(), after, SafeInteger.MAX_VALUE,
                limit + 1, Sort.Direction.ASC, status);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(SafeInteger.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private Page<SheriffComplainGroup> findSlice(UUID nodeId, long afterMoment, long beforeMoment, int limit,
                                    Sort.Direction direction, SheriffComplainStatus status) {
        Pageable pageable = PageRequest.of(0, limit + 1, direction, "moment");
        return status == null
                ? sheriffComplainGroupRepository.findInRange(nodeId, afterMoment, beforeMoment, pageable)
                : sheriffComplainGroupRepository.findByStatusInRange(
                        nodeId, afterMoment, beforeMoment, status, pageable);
    }

    private static void fillSlice(SheriffComplainGroupsSliceInfo sliceInfo, Page<SheriffComplainGroup> page) {
        sliceInfo.setGroups(page.getContent().stream().map(SheriffComplainGroupInfo::new).collect(Collectors.toList()));
    }

    private void calcSliceTotals(SheriffComplainGroupsSliceInfo sliceInfo, SheriffComplainStatus status) {
        int total = status == null
                ? sheriffComplainGroupRepository.countByNodeId(requestContext.nodeId())
                : sheriffComplainGroupRepository.countByStatus(requestContext.nodeId(), status);
        sliceInfo.setTotal(total);
        if (sliceInfo.getAfter() <= SafeInteger.MIN_VALUE) {
            sliceInfo.setTotalInPast(0);
            sliceInfo.setTotalInFuture(total - sliceInfo.getGroups().size());
        } else if (sliceInfo.getBefore() >= SafeInteger.MAX_VALUE) {
            sliceInfo.setTotalInFuture(0);
            sliceInfo.setTotalInPast(total - sliceInfo.getGroups().size());
        } else {
            int totalInFuture = status == null
                    ? sheriffComplainGroupRepository.countInRange(
                            requestContext.nodeId(), sliceInfo.getBefore(), SafeInteger.MAX_VALUE)
                    : sheriffComplainGroupRepository.countByStatusInRange(
                            requestContext.nodeId(), sliceInfo.getBefore(), SafeInteger.MAX_VALUE, status);
            sliceInfo.setTotalInFuture(totalInFuture);
            sliceInfo.setTotalInPast(total - totalInFuture - sliceInfo.getGroups().size());
        }
    }

    @GetMapping("/{id}")
    @Transactional
    public SheriffComplainGroupInfo get(@PathVariable UUID id) {
        log.info("GET /sheriff/complains/groups/{id} (id = {})", LogUtil.format(id));

        SheriffComplainGroup sheriffComplainGroup = sheriffComplainGroupRepository
                .findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("sheriff-complain-group.not-found"));

        return new SheriffComplainGroupInfo(sheriffComplainGroup);
    }

    @GetMapping("/{id}/complains")
    @Transactional
    public List<SheriffComplainInfo> getComplains(@PathVariable UUID id) {
        log.info("GET /sheriff/complains/groups/{id}/complains (id = {})", LogUtil.format(id));

        SheriffComplainGroup sheriffComplainGroup = sheriffComplainGroupRepository
                .findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("sheriff-complain-group.not-found"));

        List<SheriffComplain> sheriffComplains = sheriffComplainRepository.findByGroupId(requestContext.nodeId(), id);

        return sheriffComplains.stream()
                .filter(sc ->
                        !sheriffComplainGroup.isAnonymous()
                                || requestContext.isAdmin()
                                || requestContext.isClient(sc.getOwnerName()))
                .map(sc -> new SheriffComplainInfo(sc, false))
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    @Admin
    @Transactional
    public SheriffComplainGroupInfo put(@PathVariable UUID id,
                                        @Valid @RequestBody SheriffComplainDecisionText sheriffComplainDecisionText) {
        log.info("PUT /sheriff/complains/groups/{id} (id = {}, reject = {}, decisionCode = {})",
                LogUtil.format(id),
                LogUtil.format(sheriffComplainDecisionText.isReject()),
                LogUtil.format(Objects.toString(sheriffComplainDecisionText.getDecisionCode())));

        if (!sheriffComplainDecisionText.isReject() && sheriffComplainDecisionText.getDecisionCode() == null) {
            throw new ValidationFailure("sheriffComplainDecisionText.decisionCode.blank");
        }

        SheriffComplainGroup sheriffComplainGroup = sheriffComplainGroupRepository
                .findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("sheriff-complain-group.not-found"));
        SheriffComplainStatus prevStatus = sheriffComplainGroup.getStatus();
        boolean noOrder = (prevStatus == SheriffComplainStatus.POSTED || prevStatus == SheriffComplainStatus.PREPARED)
                && sheriffComplainDecisionText.isReject();
        sheriffComplainDecisionText.toSheriffComplainGroup(sheriffComplainGroup);

        if (!noOrder) {
            SheriffOrderAttributes attributes = new SheriffOrderAttributes(
                    sheriffComplainGroup, SheriffOrderCategory.VISIBILITY, sheriffComplainDecisionText);
            var orderTask = new SheriffOrderPostTask(sheriffComplainGroup.getRemoteNodeName(), attributes);
            taskAutowire.autowire(orderTask);
            taskExecutor.execute(orderTask);
        }

        requestContext.send(new SheriffComplainGroupUpdatedLiberin(sheriffComplainGroup, prevStatus));

        return new SheriffComplainGroupInfo(sheriffComplainGroup);
    }

}
