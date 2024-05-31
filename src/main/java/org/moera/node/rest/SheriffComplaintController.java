package org.moera.node.rest;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.data.SheriffComplaintGroupRepository;
import org.moera.node.data.SheriffComplaintRepository;
import org.moera.node.data.SheriffComplaintStatus;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SheriffComplaintAddedLiberin;
import org.moera.node.liberin.model.SheriffComplaintGroupAddedLiberin;
import org.moera.node.model.SheriffComplaintInfo;
import org.moera.node.model.SheriffComplaintText;
import org.moera.node.model.SheriffOrderReason;
import org.moera.node.rest.task.SheriffComplaintGroupPrepareJob;
import org.moera.node.task.Jobs;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/sheriff/complaints")
@NoCache
public class SheriffComplaintController {

    private static final Logger log = LoggerFactory.getLogger(SheriffComplaintController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private SheriffComplaintRepository sheriffComplaintRepository;

    @Inject
    private SheriffComplaintGroupRepository sheriffComplaintGroupRepository;

    @Inject
    private Jobs jobs;

    @Inject
    private Transaction tx;

    private final MomentFinder momentFinder = new MomentFinder();

    @PostMapping
    @Transactional
    public ResponseEntity<SheriffComplaintInfo> post(@Valid @RequestBody SheriffComplaintText sheriffComplaintText) {
        log.info("POST /sheriff/complaints"
                        + " (nodeName = {}, feedName = {}, postingId = {}, commentId = {}, reasonCode = {})",
                LogUtil.format(sheriffComplaintText.getNodeName()),
                LogUtil.format(sheriffComplaintText.getFeedName()),
                LogUtil.format(sheriffComplaintText.getPostingId()),
                LogUtil.format(sheriffComplaintText.getCommentId()),
                LogUtil.format(SheriffOrderReason.toValue(sheriffComplaintText.getReasonCode())));

        if (ObjectUtils.isEmpty(requestContext.getClientName())) {
            throw new AuthenticationException();
        }

        SheriffComplaint sheriffComplaint = new SheriffComplaint();
        sheriffComplaint.setId(UUID.randomUUID());
        sheriffComplaint.setNodeId(requestContext.nodeId());
        var groupAndCreated = findOrCreateComplaintGroup(sheriffComplaintText);
        SheriffComplaintGroup group = groupAndCreated.getFirst();
        boolean groupCreated = groupAndCreated.getSecond();
        sheriffComplaint.setGroup(group);
        sheriffComplaint.setOwnerName(requestContext.getClientName());
        sheriffComplaintText.toSheriffComplaint(sheriffComplaint);
        if (group.getStatus() != SheriffComplaintStatus.APPROVED
                && group.getStatus() != SheriffComplaintStatus.REJECTED) {
            group.setAnonymous(group.isAnonymous() || sheriffComplaint.isAnonymousRequested());
        }
        sheriffComplaint = sheriffComplaintRepository.save(sheriffComplaint);

        if (groupCreated) {
            requestContext.send(new SheriffComplaintGroupAddedLiberin(group));

            jobs.run(
                    SheriffComplaintGroupPrepareJob.class,
                    new SheriffComplaintGroupPrepareJob.Parameters(
                            group.getId(),
                            group.getRemoteNodeName(),
                            group.getRemoteFeedName(),
                            group.getRemotePostingId(),
                            group.getRemoteCommentId()),
                    requestContext.nodeId());
        }

        requestContext.send(new SheriffComplaintAddedLiberin(sheriffComplaint, group));

        return ResponseEntity.created(URI.create("/sheriff/complaints/" + sheriffComplaint.getId()))
                .body(new SheriffComplaintInfo(sheriffComplaint, true));
    }

    private Pair<SheriffComplaintGroup, Boolean> findOrCreateComplaintGroup(SheriffComplaintText sheriffComplaintText) {
        SheriffComplaintGroup group = findComplaintGroup(sheriffComplaintText).orElse(null);
        if (group != null) {
            return Pair.of(group, false);
        }
        try {
            return tx.executeWrite(() -> {
                SheriffComplaintGroup grp = new SheriffComplaintGroup();
                grp.setId(UUID.randomUUID());
                grp.setNodeId(requestContext.nodeId());
                sheriffComplaintText.toSheriffComplaintGroup(grp);
                grp.setMoment(momentFinder.find(
                        moment -> sheriffComplaintGroupRepository.countMoments(requestContext.nodeId(), moment) == 0,
                        Util.now()));
                return Pair.of(sheriffComplaintGroupRepository.save(grp), true);
            });
        } catch (DataIntegrityViolationException e) {
            return Pair.of(findComplaintGroup(sheriffComplaintText).orElseThrow(), false);
        }
    }

    private Optional<SheriffComplaintGroup> findComplaintGroup(SheriffComplaintText sheriffComplaintText) {
        if (sheriffComplaintText.getPostingId() == null) {
            return sheriffComplaintGroupRepository.findByFeed(requestContext.nodeId(),
                    sheriffComplaintText.getNodeName(), sheriffComplaintText.getFeedName());
        } else if (sheriffComplaintText.getCommentId() == null) {
            return sheriffComplaintGroupRepository.findByPosting(requestContext.nodeId(),
                    sheriffComplaintText.getNodeName(), sheriffComplaintText.getFeedName(),
                    sheriffComplaintText.getPostingId());
        } else {
            return sheriffComplaintGroupRepository.findByComment(requestContext.nodeId(),
                    sheriffComplaintText.getNodeName(), sheriffComplaintText.getFeedName(),
                    sheriffComplaintText.getPostingId(), sheriffComplaintText.getCommentId());
        }
    }

}
