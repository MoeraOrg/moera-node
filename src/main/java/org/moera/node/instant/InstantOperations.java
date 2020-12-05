package org.moera.node.instant;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.StoryRepository;
import org.moera.node.domain.Domains;
import org.moera.node.event.EventManager;
import org.moera.node.model.event.Event;
import org.moera.node.model.event.StoryDeletedEvent;
import org.moera.node.util.Transaction;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

@Component
public class InstantOperations {

    @Inject
    private Domains domains;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private EventManager eventManager;

    @Inject
    private PlatformTransactionManager txManager;

    @Scheduled(fixedDelayString = "P1D")
    public void purgeExpired() throws Throwable {
        for (String domainName : domains.getAllDomainNames()) {
            UUID nodeId = domains.getDomainNodeId(domainName);
            Duration lifetime = domains.getDomainOptions(domainName).getDuration("instants.lifetime");
            Timestamp createdBefore = Timestamp.from(Instant.now().minus(lifetime));
            List<Event> events = new ArrayList<>();
            Transaction.execute(txManager, () -> {
                storyRepository.findExpired(nodeId, "instants", createdBefore).forEach(story -> {
                    storyRepository.delete(story);
                    events.add(new StoryDeletedEvent(story, true));
                });
                return null;
            });
            events.forEach(event -> eventManager.send(nodeId, event));
        }
    }

}
