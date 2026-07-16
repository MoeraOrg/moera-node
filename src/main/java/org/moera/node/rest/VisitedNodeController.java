package org.moera.node.rest;

import java.util.Collections;
import java.util.List;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SearchNodeInfo;
import org.moera.lib.node.types.VisitedNodeAttributes;
import org.moera.lib.node.types.validate.ValidationUtil;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.config.Config;
import org.moera.node.data.FavorType;
import org.moera.node.data.QContact;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.SearchNodeInfoUtil;
import org.moera.node.operations.ContactOperations;
import org.moera.node.operations.ContactSearch;
import org.moera.node.operations.FavorOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/people/visited")
@NoCache
public class VisitedNodeController {

    private static final Logger log = LoggerFactory.getLogger(VisitedNodeController.class);

    @Inject
    private Config config;

    @Inject
    private RequestContext requestContext;

    @Inject
    private ContactOperations contactOperations;

    @Inject
    private FavorOperations favorOperations;

    @Inject
    private ContactSearch contactSearch;

    @GetMapping
    @Admin(Scope.VIEW_PEOPLE)
    @Transactional
    public List<SearchNodeInfo> getAll(
        @RequestParam(defaultValue = "") String query,
        @RequestParam(required = false) Integer limit
    ) {
        log.info("GET /people/visited (query = {}, limit = {})", LogUtil.format(query), LogUtil.format(limit));

        limit = limit != null && limit <= ContactSearch.MAX_CONTACTS_PER_REQUEST
            ? limit
            : ContactSearch.MAX_CONTACTS_PER_REQUEST;
        ValidationUtil.assertion(limit >= 0, "limit.invalid");
        if (limit == 0) {
            return Collections.emptyList();
        }

        return contactSearch
            .search(requestContext.nodeId(), query, limit, QContact.contact.visitCount.gt(0))
            .stream()
            .map(c -> SearchNodeInfoUtil.build(c, config.getMedia().getDirectServe()))
            .toList();
    }

    @PostMapping
    @Admin(Scope.OTHER)
    @Transactional
    public Result post(@RequestBody VisitedNodeAttributes node) {
        log.info("POST /people/visited (nodeName = {})", LogUtil.format(node.getNodeName()));

        node.validate();
        favorOperations.addFavor(node.getNodeName(), FavorType.VISITED);
        contactOperations.updateVisitCount(node.getNodeName(), 1);

        return Result.OK;
    }

    @DeleteMapping("/{nodeName}")
    @Admin(Scope.OTHER)
    @Transactional
    public Result delete(@PathVariable String nodeName) {
        log.info("DELETE /people/visited/{nodeName} (nodeName = {})", LogUtil.format(nodeName));

        ValidationUtil.notBlank(nodeName, "visited.node-name.blank");
        ValidationUtil.maxSize(nodeName, 135, "visited.node-name.wrong-size");
        favorOperations.deleteFavors(nodeName, FavorType.VISITED);
        contactOperations.assignVisitCount(nodeName, 0);

        return Result.OK;
    }

}
