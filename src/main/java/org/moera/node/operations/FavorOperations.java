package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import jakarta.inject.Inject;

import org.moera.node.data.Contact;
import org.moera.node.data.Favor;
import org.moera.node.data.FavorRepository;
import org.moera.node.global.UniversalContext;
import org.moera.node.task.Jobs;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;

@Component
public class FavorOperations {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private FavorRepository favorRepository;

    @Inject
    private Jobs jobs;

    public void addFavor(String nodeName, FavorType favorType) {
        addFavor(universalContext.nodeId(), nodeName, favorType);
    }

    public void addFavor(UUID nodeId, String nodeName, FavorType favorType) {
        if (nodeName == null) {
            return;
        }

        Favor favor = new Favor();
        favor.setId(UUID.randomUUID());
        favor.setNodeId(nodeId);
        favor.setNodeName(nodeName);
        favor.setValue(favorType.getValue());
        favor.setDecayHours(favorType.getDecayHours());
        favor.setDeadline(Timestamp.from(Instant.now().plus(favorType.getDecayHours(), ChronoUnit.HOURS)));
        favorRepository.save(favor);
    }

    public void asyncAddFavor(String nodeName, FavorType favorType) {
        asyncAddFavor(universalContext.nodeId(), nodeName, favorType);
    }

    public void asyncAddFavor(UUID nodeId, String nodeName, FavorType favorType) {
        if (nodeName != null) {
            jobs.runNoPersist(AddFavorJob.class, new AddFavorJob.Parameters(nodeName, favorType), nodeId);
        }
    }

    public void deleteExpired() {
        favorRepository.deleteExpired(Util.now());
    }

    public void updateDistance(Contact contact) {
        var favors = favorRepository.findByNodeName(universalContext.nodeId(), contact.getRemoteNodeName());
        double closeness = 0;
        for (Favor favor : favors) {
            long hours = favor.getCreatedAt().toInstant().until(Instant.now(), ChronoUnit.HOURS);
            if (hours <= 0) {
                continue;
            }
            double passed = hours / (double) favor.getDecayHours();
            closeness += favor.getValue() * (1f - passed * passed);
        }
        closeness = Math.max(Math.tanh(closeness / 100), 0);

        float distance = 3;
        if (contact.getFeedSubscriptionCount() > 0) {
            distance = 1;
        }
        if (contact.getFriendCount() > 0) {
            distance -= .25f;
        }
        if (contact.getBlockedUserCount() > 0) {
            distance += 1;
        }
        distance -= (float) closeness;

        contact.setDistance(distance);
    }

}
