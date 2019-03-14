package org.moera.node.rest;

import javax.inject.Inject;

import org.moera.node.global.ApiController;
import org.moera.node.model.WhoAmI;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/whoami")
public class WhoAmIiController {

    private static Logger log = LoggerFactory.getLogger(WhoAmIiController.class);

    @Inject
    private Options options;

    @GetMapping
    @ResponseBody
    public WhoAmI get() {
        log.info("GET /whoami");

        return new WhoAmI(options);
    }

}
