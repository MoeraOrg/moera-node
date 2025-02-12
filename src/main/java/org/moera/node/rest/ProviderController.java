package org.moera.node.rest;

import jakarta.inject.Inject;
import jakarta.validation.Valid;

import org.moera.lib.node.types.DeleteNodeStatus;
import org.moera.lib.node.types.Scope;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.DeleteNodeCancelledLiberin;
import org.moera.node.liberin.model.DeleteNodeRequestedLiberin;
import org.moera.node.model.DeleteNodeStatusUtil;
import org.moera.node.model.DeleteNodeText;
import org.moera.node.model.ValidationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/provider")
@NoCache
public class ProviderController {

    private static final Logger log = LoggerFactory.getLogger(ProviderController.class);

    @Inject
    private RequestContext requestContext;

    @GetMapping("/delete-node")
    @Admin(Scope.OTHER)
    public DeleteNodeStatus deleteNodeStatus() {
        log.info("GET /delete-node");

        return DeleteNodeStatusUtil.build(requestContext.getOptions());
    }

    @PostMapping("/delete-node")
    @Admin(Scope.OTHER)
    public DeleteNodeStatus deleteNode(@Valid @RequestBody DeleteNodeText deleteNodeText) {
        log.info("POST /delete-node");

        String email = requestContext.getOptions().getString("profile.email");
        if (ObjectUtils.isEmpty(email)) {
            throw new ValidationFailure("delete-node.no-email");
        }

        requestContext.getOptions().set("delete-node.requested", true);

        requestContext.send(new DeleteNodeRequestedLiberin(deleteNodeText.getMessage()));

        return DeleteNodeStatusUtil.build(requestContext.getOptions());
    }

    @DeleteMapping("/delete-node")
    @Admin(Scope.OTHER)
    public DeleteNodeStatus cancelDeleteNode() {
        log.info("DELETE /delete-node");

        boolean requested = requestContext.getOptions().getBool("delete-node.requested");
        if (requested) {
            requestContext.getOptions().set("delete-node.requested", false);
            requestContext.send(new DeleteNodeCancelledLiberin());
        }

        return DeleteNodeStatusUtil.build(requestContext.getOptions());
    }

}
