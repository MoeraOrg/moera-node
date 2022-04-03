package org.moera.node.ui;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.liberin.model.StoryUpdatedLiberin;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.RegisteredNameDetails;
import org.moera.node.util.Util;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/moera")
public class RedirectController {

    @Inject
    private RequestContext requestContext;

    @Inject
    private NamingCache namingCache;

    @Inject
    private StoryRepository storyRepository;

    private void markAsRead(UUID trackingId) {
        Story story = storyRepository.findByTrackingId(requestContext.nodeId(), trackingId).orElse(null);
        if (story == null || story.isViewed() && story.isRead()) {
            return;
        }
        story.setViewed(true);
        story.setRead(true);

        requestContext.send(new StoryUpdatedLiberin(story));
    }

    @GetMapping("/gotoname")
    @Transactional
    public String goToName(@RequestParam(required = false) String client,
                           @RequestParam(required = false) String name,
                           @RequestParam(required = false) String location,
                           @RequestParam(required = false) UUID trackingId) {
        String href = "";
        if (!ObjectUtils.isEmpty(name)) {
            RegisteredNameDetails details = namingCache.get(name);
            if (details == null || details.getNodeUri() == null) {
                throw new PageNotFoundException();
            }
            href = details.getNodeUri();
        }
        if (trackingId != null) {
            markAsRead(trackingId);
        }
        if (!ObjectUtils.isEmpty(location)) {
            href += location;
        }
        if (!ObjectUtils.isEmpty(client)) {
            href = client + "/?href=" + Util.ue(href);
        }
        return "redirect:" + (!ObjectUtils.isEmpty(href) ? href : "/");
    }

}
