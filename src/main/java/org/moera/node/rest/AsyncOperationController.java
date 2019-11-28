package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.util.LogUtil;
import org.moera.node.data.RemotePostingVerification;
import org.moera.node.data.RemotePostingVerificationRepository;
import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.RemotePostingVerificationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/async-operations")
public class AsyncOperationController {

    private static Logger log = LoggerFactory.getLogger(AsyncOperationController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private RemotePostingVerificationRepository remotePostingVerificationRepository;

    @GetMapping("/remote-posting-verification/{id}")
    @Admin
    public RemotePostingVerificationInfo getRemotePostingVerification(@PathVariable UUID id) {
        log.info("GET /async-operations/remote-posting-verification/{id}, (id = {})", LogUtil.format(id));

        RemotePostingVerification data =
                remotePostingVerificationRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
        if (data == null) {
            throw new ObjectNotFoundFailure("async-operation.not-found");
        }

        return new RemotePostingVerificationInfo(data);
    }

}