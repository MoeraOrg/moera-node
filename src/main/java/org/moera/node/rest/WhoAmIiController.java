package org.moera.node.rest;

import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Avatar;
import org.moera.node.data.AvatarRepository;
import org.moera.node.global.ApiController;
import org.moera.node.global.RequestContext;
import org.moera.node.model.WhoAmI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@ApiController
@RequestMapping("/moera/api/whoami")
public class WhoAmIiController {

    private static Logger log = LoggerFactory.getLogger(WhoAmIiController.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private AvatarRepository avatarRepository;

    private Avatar getAvatar() {
        UUID id = requestContext.getOptions().getUuid("profile.avatar.id");
        if (id == null) {
            return null;
        }
        return avatarRepository.findByNodeIdAndId(requestContext.nodeId(), id).orElse(null);
    }

    @GetMapping
    public WhoAmI get() {
        log.info("GET /whoami");

        return new WhoAmI(requestContext.getOptions(), getAvatar());
    }

}
