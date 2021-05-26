package org.moera.node.rest;

import javax.inject.Inject;

import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.model.WhoAmI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/whoami")
@NoCache
public class WhoAmIiController {

    private static Logger log = LoggerFactory.getLogger(WhoAmIiController.class);

    @Inject
    private RequestContext requestContext;

    @GetMapping
    public WhoAmI get() {
        log.info("GET /whoami");

        return new WhoAmI(requestContext);
    }

}
