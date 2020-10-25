package org.moera.node.ui;

import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Feed;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.global.RequestContext;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoryUpdatedEvent;
import org.moera.node.naming.NamingCache;
import org.moera.node.naming.RegisteredNameDetails;
import org.moera.node.operations.StoryOperations;
import org.moera.node.util.Util;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
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

    @Inject
    private StoryOperations storyOperations;

    private void markAsRead(UUID trackingId) {
        Story story = storyRepository.findByTrackingId(requestContext.nodeId(), trackingId).orElse(null);
        if (story == null || story.isRead()) {
            return;
        }
        story.setRead(true);
        if (!Feed.isAdmin(story.getFeedName())) {
            requestContext.send(new StoryUpdatedEvent(story, false));
        }
        requestContext.send(new StoryUpdatedEvent(story, true));
        requestContext.send(
                new FeedStatusUpdatedEvent(story.getFeedName(), storyOperations.getFeedStatus(story.getFeedName())));
    }

    @GetMapping("/gotoname")
    @Transactional
    public String goToName(@RequestParam String client,
                           @RequestParam String name,
                           @RequestParam(required = false) String location,
                           @RequestParam(required = false) UUID trackingId) {
        RegisteredNameDetails details = namingCache.get(name);
        if (details == null || details.getNodeUri() == null) {
            throw new PageNotFoundException();
        }
        if (trackingId != null) {
            markAsRead(trackingId);
        }
        String href = details.getNodeUri() + (location != null ? location : "");
        href = StringUtils.isEmpty(client) ? href : client + "/?href=" + Util.ue(href);
        return "redirect:" + href;
    }

    @GetMapping("/track")
    @Transactional
    public String track(@RequestParam UUID trackingId, @RequestParam String href) {
        markAsRead(trackingId);
        return "redirect:" + href;
    }

}
