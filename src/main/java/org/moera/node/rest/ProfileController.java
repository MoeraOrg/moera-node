package org.moera.node.rest;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.node.global.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.Profile;
import org.moera.node.model.ProfileInfo;
import org.moera.node.model.Result;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@ApiController
@RequestMapping("/moera/api/profile")
public class ProfileController {

    private static Logger log = LoggerFactory.getLogger(CredentialsController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private Options options;

    @GetMapping
    @ResponseBody
    public ProfileInfo get() {
        log.info("GET /profile");

        return new ProfileInfo(options, requestContext);
    }

    @PutMapping
    @Admin
    @ResponseBody
    @Transactional
    public Result put(@Valid @RequestBody Profile profile) {
        log.info("PUT /profile");

        profile.toOptions(options);
        return Result.OK;
    }

}
