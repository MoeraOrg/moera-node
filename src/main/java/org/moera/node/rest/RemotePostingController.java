package org.moera.node.rest;

import java.sql.Timestamp;
import java.time.Instant;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.AsyncOperationCreated;
import org.moera.lib.node.types.PostingSourceText;
import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SourceFormat;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.OwnPosting;
import org.moera.node.data.OwnPostingRepository;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.Entitled;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.RemotePostingUpdatedLiberin;
import org.moera.node.media.MediaOperations;
import org.moera.node.model.AsyncOperationCreatedUtil;
import org.moera.node.model.AvatarDescriptionUtil;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.operations.ContactOperations;
import org.moera.node.rest.task.RemotePostingPostJob;
import org.moera.node.rest.task.verification.RemotePostingVerifyTask;
import org.moera.node.task.Jobs;
import org.moera.node.task.TaskAutowire;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/nodes/{nodeName}/postings")
@NoCache
public class RemotePostingController {

    private static final Logger log = LoggerFactory.getLogger(RemotePostingController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private OwnPostingRepository ownPostingRepository;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @Inject
    private MediaOperations mediaOperations;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    private Jobs jobs;

    @PostMapping
    @Admin(Scope.REMOTE_ADD_POST)
    @Entitled
    @Transactional
    public Result post(@PathVariable String nodeName, @RequestBody PostingSourceText postingText) {
        log.info(
            "POST /nodes/{nodeName}/postings (nodeName = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(nodeName),
            LogUtil.format(postingText.getBodySrc().getEncoded(), 64),
            LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat()))
        );

        postingText.validate();
        update(nodeName, null, postingText);

        return Result.OK;
    }

    @PutMapping("/{postingId}")
    @Admin(Scope.REMOTE_UPDATE_POST)
    @Entitled
    @Transactional
    public Result put(
        @PathVariable String nodeName, @PathVariable String postingId, @RequestBody PostingSourceText postingText
    ) {
        log.info(
            "PUT /nodes/{nodeName}/postings/{postingId}"
                + " (nodeName = {}, postingId = {}, bodySrc = {}, bodySrcFormat = {})",
            LogUtil.format(nodeName),
            LogUtil.format(postingId),
            LogUtil.format(postingText.getBodySrc().getEncoded(), 64),
            LogUtil.format(SourceFormat.toValue(postingText.getBodySrcFormat()))
        );

        postingText.validate();
        update(nodeName, postingId, postingText);

        return Result.OK;
    }

    private void update(String nodeName, String postingId, PostingSourceText postingText) {
        mediaOperations.validateAvatar(
            postingText.getOwnerAvatar(),
            mf -> AvatarDescriptionUtil.setMediaFile(postingText.getOwnerAvatar(), mf),
            () -> new ObjectNotFoundFailure("avatar.not-found")
        );
        jobs.run(
            RemotePostingPostJob.class,
            new RemotePostingPostJob.Parameters(nodeName, postingId, postingText),
            requestContext.nodeId()
        );
    }

    @DeleteMapping("/{postingId}")
    @Admin(Scope.REMOTE_DELETE_CONTENT)
    @Transactional
    public Result delete(@PathVariable String nodeName, @PathVariable String postingId) {
        log.info(
            "DELETE /nodes/{nodeName}/postings/{postingId} (nodeName = {}, postingId = {})",
            LogUtil.format(nodeName), LogUtil.format(postingId)
        );

        OwnPosting ownPosting = ownPostingRepository
            .findByRemotePostingId(requestContext.nodeId(), nodeName, postingId)
            .orElse(null);
        if (ownPosting != null) {
            contactOperations.asyncUpdateCloseness(nodeName, -1);
            requestContext.send(new RemotePostingUpdatedLiberin(nodeName, postingId));
        }

        return Result.OK;
    }

    @PostMapping("/{id}/verify")
    @Admin(Scope.OTHER)
    @Transactional
    public AsyncOperationCreated verify(@PathVariable String nodeName, @PathVariable String id) {
        log.info(
            "POST /nodes/{name}/postings/{id}/verify, (name = {}, id = {})",
            LogUtil.format(nodeName), LogUtil.format(id)
        );

        return executeVerifyTask(nodeName, id, null);
    }

    @PostMapping("/{id}/revisions/{revisionId}/verify")
    @Admin(Scope.OTHER)
    @Transactional
    public AsyncOperationCreated verifyRevision(
        @PathVariable String nodeName, @PathVariable String id, @PathVariable String revisionId
    ) {
        log.info(
            "POST /nodes/{name}/postings/{id}/revisions/{revisionId}/verify, (name = {}, id = {}, revisionId = {})",
            LogUtil.format(nodeName), LogUtil.format(id), LogUtil.format(revisionId)
        );

        return executeVerifyTask(nodeName, id, revisionId);
    }

    private AsyncOperationCreated executeVerifyTask(String nodeName, String id, String revisionId) {
        RemotePostingVerification data = new RemotePostingVerification(
            requestContext.nodeId(), nodeName, id, revisionId
        );
        data.setDeadline(Timestamp.from(
            Instant.now().plus(
                requestContext.getOptions().getDuration("remote-posting-verification.lifetime").getDuration()
            )
        ));
        remotePostingVerificationRepository.saveAndFlush(data);

        RemotePostingVerifyTask task = new RemotePostingVerifyTask(data);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);

        return AsyncOperationCreatedUtil.build(data.getId());
    }

}
