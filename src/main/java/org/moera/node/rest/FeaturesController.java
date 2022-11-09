package org.moera.node.rest;

import javax.inject.Inject;

import org.moera.node.friends.FriendCache;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Features;
import org.moera.node.plugin.Plugins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/features")
@NoCache
public class FeaturesController {

    private static final Logger log = LoggerFactory.getLogger(FeaturesController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Plugins plugins;

    @Inject
    private FriendCache friendCache;

    @GetMapping
    public Features get() {
        log.info("GET /features");

        return new Features(requestContext.getOptions(), plugins.getNames(requestContext.nodeId()),
                friendCache.getNodeGroups(), friendCache.getClientGroups(requestContext.getClientName()),
                requestContext.isAdmin());
    }

}
