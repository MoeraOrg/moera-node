package org.moera.node.rest;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.node.types.GrantInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.lib.naming.NodeName;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.auth.Admin;
import org.moera.node.data.GrantRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.GrantUpdatedLiberin;
import org.moera.node.model.GrantChange;
import org.moera.node.model.GrantInfoUtil;
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

    @Inject
    private GrantRepository grantRepository;

    @GetMapping
    @Admin(Scope.OTHER)
    @Transactional
    public List<GrantInfo> getAll() {
        log.info("GET /grants");

        return grantRepository.findAllByNodeId(requestContext.nodeId()).stream().map(GrantInfoUtil::build).toList();
    }

    @GetMapping("/{nodeName}")
    @Admin(Scope.OTHER)
    @Transactional
    public GrantInfo get(@PathVariable String nodeName) {
        log.info("GET /grants/{nodeName}, (nodeName = {})", LogUtil.format(nodeName));

        nodeName = NodeName.expand(nodeName);
        long scope = grantCache.get(requestContext.nodeId(), nodeName);

        return GrantInfoUtil.build(nodeName, scope);
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

        return GrantInfoUtil.build(nodeName, scope);
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
