package org.moera.node.rest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SheriffComplaintGroupInfo;
import org.moera.lib.node.types.SheriffComplaintGroupsSliceInfo;
import org.moera.lib.node.types.SheriffComplaintInfo;
import org.moera.lib.node.types.SheriffComplaintStatus;
import org.moera.lib.node.types.SheriffOrderCategory;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.data.SheriffComplaintGroupRepository;
import org.moera.node.data.SheriffComplaintRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SheriffComplaintGroupUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.SheriffComplaintDecisionText;
import org.moera.node.model.SheriffComplaintGroupInfoUtil;
import org.moera.node.model.SheriffComplaintInfoUtil;
import org.moera.node.model.SheriffOrderAttributes;
import org.moera.node.model.ValidationFailure;
import org.moera.node.rest.task.SheriffOrderPostJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.SafeInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping("/moera/api/sheriff/complaints/groups")
@NoCache
public class SheriffComplaintGroupController {

    private static final Logger log = LoggerFactory.getLogger(SheriffComplaintGroupController.class);

    private static final int MAX_GROUPS_PER_REQUEST = 200;

    @Inject
    private RequestContext requestContext;

    @Inject
    private SheriffComplaintGroupRepository sheriffComplaintGroupRepository;

    @Inject
    private SheriffComplaintRepository sheriffComplaintRepository;

    @Inject
    private Jobs jobs;

    @GetMapping
    @Transactional
    public SheriffComplaintGroupsSliceInfo getAll(
            @RequestParam(required = false) Long before,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) SheriffComplaintStatus status
    ) {
        log.info("GET /sheriff/complaints/groups (before = {}, after = {}, limit = {}, status = {})",
                LogUtil.format(before), LogUtil.format(after), LogUtil.format(limit),
                LogUtil.format(Objects.toString(status, null)));

        if (before != null && after != null) {
            throw new ValidationFailure("sheriff-complaint-groups.before-after-exclusive");
        }

        limit = limit != null && limit <= MAX_GROUPS_PER_REQUEST ? limit : MAX_GROUPS_PER_REQUEST;
        if (limit < 0) {
            throw new ValidationFailure("limit.invalid");
        }

        SheriffComplaintGroupsSliceInfo sliceInfo;
        if (after == null) {
            before = before != null ? before : SafeInteger.MAX_VALUE;
            sliceInfo = getGroupsBefore(before, limit, status);
        } else {
            sliceInfo = getGroupsAfter(after, limit, status);
        }
        calcSliceTotals(sliceInfo, status);

        return sliceInfo;
    }

    private SheriffComplaintGroupsSliceInfo getGroupsBefore(long before, int limit, SheriffComplaintStatus status) {
        SheriffComplaintGroupsSliceInfo sliceInfo = new SheriffComplaintGroupsSliceInfo();
        sliceInfo.setBefore(before);
        Page<SheriffComplaintGroup> page = findSlice(requestContext.nodeId(), SafeInteger.MIN_VALUE, before,
                limit + 1, Sort.Direction.DESC, status);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setAfter(SafeInteger.MIN_VALUE);
        } else {
            sliceInfo.setAfter(page.getContent().get(limit).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private SheriffComplaintGroupsSliceInfo getGroupsAfter(long after, int limit, SheriffComplaintStatus status) {
        SheriffComplaintGroupsSliceInfo sliceInfo = new SheriffComplaintGroupsSliceInfo();
        sliceInfo.setAfter(after);
        Page<SheriffComplaintGroup> page = findSlice(requestContext.nodeId(), after, SafeInteger.MAX_VALUE,
                limit + 1, Sort.Direction.ASC, status);
        if (page.getNumberOfElements() < limit + 1) {
            sliceInfo.setBefore(SafeInteger.MAX_VALUE);
        } else {
            sliceInfo.setBefore(page.getContent().get(limit - 1).getMoment());
        }
        fillSlice(sliceInfo, page);
        return sliceInfo;
    }

    private Page<SheriffComplaintGroup> findSlice(UUID nodeId, long afterMoment, long beforeMoment, int limit,
                                                  Sort.Direction direction, SheriffComplaintStatus status) {
        Pageable pageable = PageRequest.of(0, limit + 1, direction, "moment");
        return status == null
                ? sheriffComplaintGroupRepository.findInRange(nodeId, afterMoment, beforeMoment, pageable)
                : sheriffComplaintGroupRepository.findByStatusInRange(
                        nodeId, afterMoment, beforeMoment, status, pageable);
    }

    private static void fillSlice(SheriffComplaintGroupsSliceInfo sliceInfo, Page<SheriffComplaintGroup> page) {
        sliceInfo.setGroups(
            page.getContent().stream()
                .map(SheriffComplaintGroupInfoUtil::build)
                .collect(Collectors.toList())
        );
    }

    private void calcSliceTotals(SheriffComplaintGroupsSliceInfo sliceInfo, SheriffComplaintStatus status) {
        int total = status == null
                ? sheriffComplaintGroupRepository.countByNodeId(requestContext.nodeId())
                : sheriffComplaintGroupRepository.countByStatus(requestContext.nodeId(), status);
        sliceInfo.setTotal(total);
        if (sliceInfo.getAfter() <= SafeInteger.MIN_VALUE) {
            sliceInfo.setTotalInPast(0);
            sliceInfo.setTotalInFuture(total - sliceInfo.getGroups().size());
        } else if (sliceInfo.getBefore() >= SafeInteger.MAX_VALUE) {
            sliceInfo.setTotalInFuture(0);
            sliceInfo.setTotalInPast(total - sliceInfo.getGroups().size());
        } else {
            int totalInFuture = status == null
                    ? sheriffComplaintGroupRepository.countInRange(
                            requestContext.nodeId(), sliceInfo.getBefore(), SafeInteger.MAX_VALUE)
                    : sheriffComplaintGroupRepository.countByStatusInRange(
                            requestContext.nodeId(), sliceInfo.getBefore(), SafeInteger.MAX_VALUE, status);
            sliceInfo.setTotalInFuture(totalInFuture);
            sliceInfo.setTotalInPast(total - totalInFuture - sliceInfo.getGroups().size());
        }
    }

    @GetMapping("/{id}")
    @Transactional
    public SheriffComplaintGroupInfo get(@PathVariable UUID id) {
        log.info("GET /sheriff/complaints/groups/{id} (id = {})", LogUtil.format(id));

        SheriffComplaintGroup sheriffComplaintGroup = sheriffComplaintGroupRepository
                .findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("sheriff-complaint-group.not-found"));

        return SheriffComplaintGroupInfoUtil.build(sheriffComplaintGroup);
    }

    @GetMapping("/{id}/complaints")
    @Transactional
    public List<SheriffComplaintInfo> getComplaints(@PathVariable UUID id) {
        log.info("GET /sheriff/complaints/groups/{id}/complaints (id = {})", LogUtil.format(id));

        SheriffComplaintGroup sheriffComplaintGroup = sheriffComplaintGroupRepository
                .findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("sheriff-complaint-group.not-found"));

        List<SheriffComplaint> sheriffComplaints = sheriffComplaintRepository.findByGroupId(requestContext.nodeId(), id);

        return sheriffComplaints.stream()
                .filter(
                    sc -> !sheriffComplaintGroup.isAnonymous()
                        || requestContext.isAdmin(Scope.SHERIFF)
                        || requestContext.isClient(sc.getOwnerName(), Scope.IDENTIFY)
                )
                .map(sc -> SheriffComplaintInfoUtil.build(sc, false))
                .collect(Collectors.toList());
    }

    @PutMapping("/{id}")
    @Admin(Scope.SHERIFF)
    @Transactional
    public SheriffComplaintGroupInfo put(@PathVariable UUID id,
                                         @Valid @RequestBody SheriffComplaintDecisionText sheriffComplaintDecisionText) {
        log.info("PUT /sheriff/complaints/groups/{id} (id = {}, reject = {}, decisionCode = {})",
                LogUtil.format(id),
                LogUtil.format(sheriffComplaintDecisionText.isReject()),
                LogUtil.format(Objects.toString(sheriffComplaintDecisionText.getDecisionCode(), null)));

        if (!sheriffComplaintDecisionText.isReject() && sheriffComplaintDecisionText.getDecisionCode() == null) {
            throw new ValidationFailure("sheriffComplaintDecisionText.decisionCode.blank");
        }

        SheriffComplaintGroup sheriffComplaintGroup = sheriffComplaintGroupRepository
                .findByNodeIdAndId(requestContext.nodeId(), id)
                .orElseThrow(() -> new ObjectNotFoundFailure("sheriff-complaint-group.not-found"));
        SheriffComplaintStatus prevStatus = sheriffComplaintGroup.getStatus();
        boolean noOrder = (prevStatus == SheriffComplaintStatus.POSTED || prevStatus == SheriffComplaintStatus.PREPARED)
                && sheriffComplaintDecisionText.isReject();
        sheriffComplaintDecisionText.toSheriffComplaintGroup(sheriffComplaintGroup);

        if (!noOrder) {
            jobs.run(
                SheriffOrderPostJob.class,
                new SheriffOrderPostJob.Parameters(
                    sheriffComplaintGroup.getRemoteNodeName(),
                    new SheriffOrderAttributes(
                        sheriffComplaintGroup,
                        SheriffOrderCategory.VISIBILITY,
                        sheriffComplaintDecisionText
                    ),
                    sheriffComplaintGroup.getId()
                ),
                requestContext.nodeId()
            );
        }

        requestContext.send(new SheriffComplaintGroupUpdatedLiberin(sheriffComplaintGroup, prevStatus));

        return SheriffComplaintGroupInfoUtil.build(sheriffComplaintGroup);
    }

}
