package org.moera.node.rest;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.moera.node.auth.Admin;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.ProfileAttributes;
import org.moera.node.model.ProfileInfo;
import org.moera.node.model.event.NodeNameChangedEvent;
import org.moera.node.model.event.ProfileUpdatedEvent;
import org.moera.node.model.notification.ProfileUpdatedNotification;
import org.moera.node.notification.send.Directions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/profile")
public class ProfileController {

    private static Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Inject
    private RequestContext requestContext;

    @GetMapping
    public ProfileInfo get() {
        log.info("GET /profile");

        return new ProfileInfo(requestContext);
    }

    @PutMapping
    @Admin
    @Transactional
    public ProfileInfo put(@Valid @RequestBody ProfileAttributes profileAttributes) {
        log.info("PUT /profile");

        profileAttributes.toOptions(requestContext.getOptions());
        requestContext.send(new ProfileUpdatedEvent());
        requestContext.send(new NodeNameChangedEvent(requestContext.nodeName(), requestContext.fullName()));
        requestContext.send(Directions.profileSubscribers(), new ProfileUpdatedNotification());
        return new ProfileInfo(requestContext);
    }

}
