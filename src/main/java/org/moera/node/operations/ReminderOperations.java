package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.node.data.Feed;
import org.moera.node.data.Reminder;
import org.moera.node.data.ReminderRepository;
import org.moera.node.data.Story;
import org.moera.node.data.StoryRepository;
import org.moera.node.data.StoryType;
import org.moera.node.domain.Domains;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.StoryAddedLiberin;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ReminderOperations {

    private static final StoryType[] REMINDERS_ALL = new StoryType[] {
        StoryType.REMINDER_FULL_NAME
    };

    private static final Duration REMINDERS_START_DELAY = Duration.ofDays(7);
    private static final Duration REMINDERS_INTERVAL = Duration.ofDays(2);
    private static final Duration REMINDER_INTERVAL = Duration.ofDays(14);
    private static final int REPEAT_COUNT = 4;

    @Inject
    private Domains domains;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private ReminderRepository reminderRepository;

    @Inject
    private StoryRepository storyRepository;

    @Inject
    private StoryOperations storyOperations;

    @Inject
    private Transaction tx;

    public void initializeNode(UUID nodeId) {
        tx.executeWrite(() -> {
            for (StoryType storyType : REMINDERS_ALL) {
                Reminder reminder = new Reminder();
                reminder.setId(UUID.randomUUID());
                reminder.setNodeId(nodeId);
                reminder.setStoryType(storyType);
                reminder.setPriority(storyType.getPriority());
                reminder.setNextAt(Timestamp.from(Instant.now().plus(REMINDERS_START_DELAY)));
                reminderRepository.save(reminder);
            }
        });
    }

    private void activateReminders(UUID nodeId) {
        universalContext.associate(nodeId);

        tx.executeWrite(() -> {
            List<Reminder> reminders = reminderRepository.findAllByNodeId(nodeId);
            if (reminders.isEmpty()) {
                return;
            }

            boolean tooOften = reminders.stream()
                    .map(Reminder::getReadAt)
                    .filter(Objects::nonNull)
                    .anyMatch(readAt -> readAt.toInstant().isAfter(Instant.now().minus(REMINDERS_INTERVAL)));
            if (tooOften) {
                return;
            }

            if (isPublishing(reminders)) {
                return;
            }

            for (Reminder reminder : reminders) {
                if (reminder.getNextAt().toInstant().isAfter(Instant.now())) {
                    continue;
                }
                if (conditionSatisfied(reminder.getStoryType())) {
                    reminderRepository.delete(reminder);
                    continue;
                }
                publish(reminder);
                return;
            }
        });
    }

    private boolean isPublishing(List<Reminder> reminders) {
        boolean publishing = false;
        for (Reminder reminder : reminders) {
            if (reminder.getStory() == null) {
                continue;
            }
            publishing = true;
            if (reminder.getStory().isRead()) {
                reminder.setReadCount(reminder.getReadCount() + 1);
                if (reminder.getReadCount() >= REPEAT_COUNT) {
                    reminderRepository.delete(reminder);
                    continue;
                }
                reminder.setReadAt(Util.now());
                reminder.setStory(null);
                Duration interval = Util.mulPow2(REMINDER_INTERVAL, reminder.getReadCount());
                reminder.setNextAt(Timestamp.from(Instant.now().plus(interval)));
            }
        }
        return publishing;
    }

    private boolean conditionSatisfied(StoryType storyType) {
        return switch (storyType) {
            case REMINDER_FULL_NAME -> !ObjectUtils.isEmpty(universalContext.fullName());
            default -> true;
        };
    }

    private void publish(Reminder reminder) {
        Story story = new Story(UUID.randomUUID(), universalContext.nodeId(), reminder.getStoryType());
        story.setFeedName(Feed.NEWS);
        storyOperations.updateMoment(story, universalContext.nodeId());
        story = storyRepository.saveAndFlush(story);
        universalContext.send(new StoryAddedLiberin(story));

        reminder.setPublishedAt(Util.now());
        reminder.setStory(story);
        reminderRepository.save(reminder);
    }

    @Scheduled(fixedDelayString = "PT6H")
    public void activateReminders() {
        domains.getWarmDomainNames().forEach(name -> activateReminders(domains.getDomainNodeId(name)));
    }

}
