package org.moera.node.instant;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.Feed;
import org.moera.node.data.StoryRepository;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.global.RequestContext;
import org.moera.node.model.event.FeedStatusUpdatedEvent;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.operations.StoryOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InstantOperations {

    @Inject
    private RequestContext requestContext;

    @Inject
    private Domains domains;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private EventManager eventManager;

    public void feedStatusUpdated() {
        requestContext.send(new FeedStatusUpdatedEvent(Feed.INSTANT, storyOperations.getFeedStatus(Feed.INSTANT)));
    }

    @Scheduled(fixedDelayString = "P1D")
    @Transactional
    public void purgeExpired() {
        for (String domainName : domains.getAllDomainNames()) {
            UUID nodeId = domains.getDomainNodeId(domainName);
            Duration lifetime = domains.getDomainOptions(domainName).getDuration("instants.lifetime");
            Timestamp createdBefore = Timestamp.from(Instant.now().minus(lifetime));
            storyRepository.findExpired(nodeId, "instants", createdBefore).forEach(story -> {
                storyRepository.delete(story);
                eventManager.send(nodeId, new StoryDeletedEvent(story, true));
            });
        }
    }

}
