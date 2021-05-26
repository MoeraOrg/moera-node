package org.moera.node.rest;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.Admin;
import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.NoCache;
import org.moera.node.global.RequestContext;
import org.moera.node.mail.EmailConfirmMail;
import org.moera.node.model.ProfileAttributes;
import org.moera.node.model.ProfileInfo;
import org.moera.node.model.event.NodeNameChangedEvent;
import org.moera.node.model.event.ProfileUpdatedEvent;
import org.moera.node.model.notification.ProfileUpdatedNotification;
import org.moera.node.notification.send.Directions;
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

    private static Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private AvatarRepository avatarRepository;

    @Inject
    private TextConverter textConverter;

    private Avatar getAvatar() {
        UUID id = requestContext.getOptions().getUuid("profile.avatar.id");
        if (id == null) {
            return null;
        }
        return avatarRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
    }

    @GetMapping
    public ProfileInfo get(@RequestParam(required = false) String include) {
        log.info("GET /profile (include = {})", LogUtil.format(include));

        Set<String> includeSet = Util.setParam(include);

        return new ProfileInfo(requestContext, includeSet.contains("source"));
    }

    @PutMapping
    @Admin
    @Transactional
    public ProfileInfo put(@Valid @RequestBody ProfileAttributes profileAttributes) {
        log.info("PUT /profile");

        String oldEmail = requestContext.getOptions().getString("profile.email");
        profileAttributes.toOptions(requestContext.getOptions(), textConverter);
        requestContext.send(new ProfileUpdatedEvent());
        requestContext.send(new NodeNameChangedEvent(requestContext.nodeName(), requestContext.getOptions(),
                requestContext.getAvatar()));
        requestContext.send(Directions.profileSubscribers(), new ProfileUpdatedNotification());
        if (!Objects.equals(requestContext.getOptions().getString("profile.email"), oldEmail)) {
            requestContext.send(new EmailConfirmMail());
        }

        return new ProfileInfo(requestContext, true);
    }

}
