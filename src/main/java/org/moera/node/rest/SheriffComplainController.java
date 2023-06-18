package org.moera.node.rest;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.SheriffComplain;
import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainGroupRepository;
import org.moera.node.data.SheriffComplainRepository;
import org.moera.node.data.SheriffComplainStatus;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.SheriffComplainAddedLiberin;
import org.moera.node.liberin.model.SheriffComplainGroupAddedLiberin;
import org.moera.node.model.SheriffComplainInfo;
import org.moera.node.model.SheriffComplainText;
import org.moera.node.model.SheriffOrderReason;
import org.moera.node.rest.task.SheriffComplainGroupPrepareTask;
import org.moera.node.task.TaskAutowire;
import org.moera.node.util.MomentFinder;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.util.Pair;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
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
    private SheriffComplainGroupRepository sheriffComplainGroupRepository;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private PlatformTransactionManager txManager;

    private final MomentFinder momentFinder = new MomentFinder();

    @PostMapping
    @Transactional
    public ResponseEntity<SheriffComplainInfo> post(@Valid @RequestBody SheriffComplainText sheriffComplainText)
            throws Throwable {

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
        var groupAndCreated = findOrCreateComplainGroup(sheriffComplainText);
        SheriffComplainGroup group = groupAndCreated.getFirst();
        boolean groupCreated = groupAndCreated.getSecond();
        sheriffComplain.setGroup(group);
        sheriffComplain.setOwnerName(requestContext.getClientName());
        sheriffComplainText.toSheriffComplain(sheriffComplain);
        if (group.getStatus() != SheriffComplainStatus.APPROVED
                && group.getStatus() != SheriffComplainStatus.REJECTED) {
            group.setAnonymous(group.isAnonymous() || sheriffComplain.isAnonymousRequested());
        }
        sheriffComplain = sheriffComplainRepository.save(sheriffComplain);

        if (groupCreated) {
            requestContext.send(new SheriffComplainGroupAddedLiberin(group));

            var prepareTask = new SheriffComplainGroupPrepareTask(group.getId(), group.getRemoteNodeName(),
                    group.getRemoteFeedName(), group.getRemotePostingId(), group.getRemoteCommentId());
            taskAutowire.autowire(prepareTask);
            taskExecutor.execute(prepareTask);
        }

        requestContext.send(new SheriffComplainAddedLiberin(sheriffComplain, group));

        return ResponseEntity.created(URI.create("/sheriff/complains/" + sheriffComplain.getId()))
                .body(new SheriffComplainInfo(sheriffComplain, true));
    }

    private Pair<SheriffComplainGroup, Boolean> findOrCreateComplainGroup(SheriffComplainText sheriffComplainText)
            throws Throwable {
        SheriffComplainGroup group = findComplainGroup(sheriffComplainText).orElse(null);
        if (group != null) {
            return Pair.of(group, false);
        }
        try {
            return Transaction.execute(txManager, () -> {
                SheriffComplainGroup grp = new SheriffComplainGroup();
                grp.setId(UUID.randomUUID());
                grp.setNodeId(requestContext.nodeId());
                sheriffComplainText.toSheriffComplainGroup(grp);
                grp.setMoment(momentFinder.find(
                        moment -> sheriffComplainGroupRepository.countMoments(requestContext.nodeId(), moment) == 0,
                        Util.now()));
                return Pair.of(sheriffComplainGroupRepository.save(grp), true);
            });
        } catch (DataIntegrityViolationException e) {
            return Pair.of(findComplainGroup(sheriffComplainText).orElseThrow(), false);
        }
    }

    private Optional<SheriffComplainGroup> findComplainGroup(SheriffComplainText sheriffComplainText) {
        if (sheriffComplainText.getPostingId() == null) {
            return sheriffComplainGroupRepository.findByFeed(requestContext.nodeId(),
                    sheriffComplainText.getNodeName(), sheriffComplainText.getFeedName());
        } else if (sheriffComplainText.getCommentId() == null) {
            return sheriffComplainGroupRepository.findByPosting(requestContext.nodeId(),
                    sheriffComplainText.getNodeName(), sheriffComplainText.getFeedName(),
                    sheriffComplainText.getPostingId());
        } else {
            return sheriffComplainGroupRepository.findByComment(requestContext.nodeId(),
                    sheriffComplainText.getNodeName(), sheriffComplainText.getFeedName(),
                    sheriffComplainText.getPostingId(), sheriffComplainText.getCommentId());
        }
    }

}
