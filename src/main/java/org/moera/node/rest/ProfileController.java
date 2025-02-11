package org.moera.node.rest;

import java.util.Set;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.ProfileReadLiberin;
import org.moera.node.liberin.model.ProfileUpdatedLiberin;
import org.moera.node.model.ProfileAttributes;
import org.moera.node.model.ProfileInfo;
import org.moera.node.operations.OperationsValidator;
import org.moera.node.text.TextConverter;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@ApiController
@RequestMapping("/moera/api/profile")
@NoCache
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private TextConverter textConverter;

    @GetMapping
    public ProfileInfo get(@RequestParam(required = false) String include) {
        log.info("GET /profile (include = {})", LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        requestContext.send(new ProfileReadLiberin());

        return new ProfileInfo(requestContext, includeSet.contains("source"));
    }

    @PutMapping
    @Admin(Scope.UPDATE_PROFILE)
    @Transactional
    public ProfileInfo put(@Valid @RequestBody ProfileAttributes profileAttributes) {
        log.info("PUT /profile");

        OperationsValidator.validateOperations(profileAttributes::getPrincipal, OperationsValidator.PROFILE_OPERATIONS,
                true, "profileAttributes.operations.wrong-principal");

        String oldEmail = requestContext.getOptions().getString("profile.email");
        profileAttributes.toOptions(requestContext.getOptions(), textConverter);

        requestContext.send(new ProfileUpdatedLiberin(requestContext.nodeName(), requestContext.getOptions(),
                requestContext.getAvatar(), oldEmail));

        return new ProfileInfo(requestContext, true);
    }

}
