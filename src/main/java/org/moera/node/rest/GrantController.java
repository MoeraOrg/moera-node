package org.moera.node.rest;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.naming.rpc.NodeName;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.auth.Admin;
import org.moera.node.auth.Scope;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.GrantUpdatedLiberin;
import org.moera.node.model.GrantChange;
import org.moera.node.model.GrantInfo;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.operations.GrantCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/grants")
@NoCache
public class GrantController {

    private static final Logger log = LoggerFactory.getLogger(GrantController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingCache namingCache;

    @Inject
    private GrantCache grantCache;

    @GetMapping("/{nodeName}")
    @Admin(Scope.OTHER)
    @Transactional
    public GrantInfo get(@PathVariable String nodeName) {
        log.info("GET /grants/{nodeName}, (nodeName = {})", LogUtil.format(nodeName));

        nodeName = NodeName.expand(nodeName);
        long scope = grantCache.get(requestContext.nodeId(), nodeName);

        return new GrantInfo(nodeName, scope);
    }

    @PutMapping("/{nodeName}")
    @Admin(Scope.GRANT)
    @Transactional
    public GrantInfo put(@PathVariable String nodeName, @Valid @RequestBody GrantChange change) {
        log.info("PUT /grants/{nodeName}, (nodeName = {})", LogUtil.format(nodeName));

        nodeName = NodeName.expand(nodeName);

        if (namingCache.get(nodeName).getNodeName() == null) {
            throw new ValidationFailure("grant.name-not-registered");
        }

        long scope;
        if (!change.isRevoke()) {
            scope = grantCache.grant(requestContext.nodeId(), nodeName, Scope.forValues(change.getScope()));
        } else {
            scope = grantCache.revoke(requestContext.nodeId(), nodeName, Scope.forValues(change.getScope()));
        }

        requestContext.send(new GrantUpdatedLiberin(nodeName, scope));

        return new GrantInfo(nodeName, scope);
    }

    @DeleteMapping("/{nodeName}")
    @Admin(Scope.GRANT)
    @Transactional
    public Result delete(@PathVariable String nodeName) {
        log.info("DELETE /grants/{nodeName}, (nodeName = {})", LogUtil.format(nodeName));

        nodeName = NodeName.expand(nodeName);

        long scope = grantCache.get(requestContext.nodeId(), nodeName);
        if (scope != 0) {
            grantCache.revoke(requestContext.nodeId(), nodeName, scope);
        }

        requestContext.send(new GrantUpdatedLiberin(nodeName, 0));

        return Result.OK;
    }

}
