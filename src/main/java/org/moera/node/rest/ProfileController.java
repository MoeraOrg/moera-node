package org.moera.node.rest;

import java.util.Set;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.moera.lib.node.types.ProfileAttributes;
import org.moera.lib.node.types.ProfileInfo;
import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.AvatarRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.ProfileReadLiberin;
import org.moera.node.liberin.model.ProfileUpdatedLiberin;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.ProfileAttributesUtil;
import org.moera.node.model.ProfileInfoUtil;
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

    @Inject
    private AvatarRepository avatarRepository;

    @GetMapping
    public ProfileInfo get(@RequestParam(required = false) String include) {
        log.info("GET /profile (include = {})", LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        requestContext.send(new ProfileReadLiberin());

        return ProfileInfoUtil.build(requestContext, includeSet.contains("source"));
    }

    @PutMapping
    @Admin(Scope.UPDATE_PROFILE)
    @Transactional
    public ProfileInfo put(@RequestBody ProfileAttributes profileAttributes) {
        log.info("PUT /profile");

        profileAttributes.validate();
        OperationsValidator.validateOperations(
            profileAttributes.getOperations(),
            true,
            "profile.operations.wrong-principal"
        );

        if (profileAttributes.getAvatarId() != null) {
            UUID avatarId = Util.uuid(profileAttributes.getAvatarId())
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
            avatarRepository.findByNodeIdAndId(requestContext.nodeId(), avatarId)
                .orElseThrow(() -> new ObjectNotFoundFailure("avatar.not-found"));
        }

        String oldEmail = requestContext.getOptions().getString("profile.email");
        ProfileAttributesUtil.toOptions(profileAttributes, requestContext.getOptions(), textConverter);

        requestContext.send(
            new ProfileUpdatedLiberin(
                requestContext.nodeName(), requestContext.getOptions(), requestContext.getAvatar(), oldEmail
            )
        );

        return ProfileInfoUtil.build(requestContext, true);
    }

}
