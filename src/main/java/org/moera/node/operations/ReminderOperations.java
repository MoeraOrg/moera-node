package org.moera.node.operations;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
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
import org.moera.node.liberin.model.StoryDeletedLiberin;
import org.moera.node.option.OptionHook;
import org.moera.node.option.OptionValueChange;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ReminderOperations {

    private static final Duration REMINDERS_START_DELAY = Duration.ofDays(7);
    private static final Duration REMINDERS_INTERVAL = Duration.ofDays(3);
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
    private BlockedInstantOperations blockedInstantOperations;

    @Inject
    private Transaction tx;

    public void initializeNode(UUID nodeId) {
        tx.executeWrite(() -> {
            for (StoryType storyType : StoryType.values()) {
                if (!storyType.isReminder()) {
                    continue;
                }
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

        if (universalContext.getOptions() == null) {
            return;
        }

        Timestamp lastInteraction = getLastInteraction();
        if (lastInteraction != null && lastInteraction.toInstant().isAfter(Instant.now().minus(REMINDERS_INTERVAL))) {
            return;
        }

        tx.executeWrite(() -> {
            List<Reminder> reminders = reminderRepository.findAllByNodeId(nodeId);
            if (reminders.isEmpty()) {
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
                    unpublishAndDelete(reminder.getStoryType());
                    continue;
                }
                if (blockedInstantOperations.count(nodeId, reminder.getStoryType()) > 0) {
                    unpublishAndDelete(reminder.getStoryType());
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
            if (reminder.getStory().isViewed()) {
                reminder.setReadCount(reminder.getReadCount() + 1);
                if (reminder.getReadCount() >= REPEAT_COUNT) {
                    reminderRepository.delete(reminder);
                    continue;
                }
                reminder.setReadAt(Util.now());
                reminder.setStory(null);
                Duration interval = Util.mulPow2(REMINDER_INTERVAL, reminder.getReadCount());
                reminder.setNextAt(Timestamp.from(Instant.now().plus(interval)));

                updateLastInteraction();
            }
        }
        return publishing;
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

        updateLastInteraction();
    }

    public void unpublishAndDelete(StoryType storyType) {
        Collection<Story> stories = storyRepository.findByFeedAndType(universalContext.nodeId(), Feed.NEWS, storyType);
        for (Story story : stories) {
            storyRepository.delete(story);
            universalContext.send(new StoryDeletedLiberin(story));
        }
        reminderRepository.deleteByNodeIdAndStoryType(universalContext.nodeId(), storyType);

        updateLastInteraction();
    }

    private Timestamp getLastInteraction() {
        return universalContext.getOptions().getTimestamp("reminder.last-interaction");
    }

    private void updateLastInteraction() {
        universalContext.getOptions().set("reminder.last-interaction", Util.now());
    }

    @Scheduled(fixedDelayString = "PT6H")
    public void activateReminders() {
        domains.getWarmDomainNames().forEach(name -> activateReminders(domains.getDomainNodeId(name)));
    }

    /* Reminder-specific routines */

    private boolean conditionSatisfied(StoryType storyType) {
        return switch (storyType) {
            case REMINDER_FULL_NAME -> !ObjectUtils.isEmpty(universalContext.fullName());
            case REMINDER_AVATAR -> universalContext.avatarId() != null;
            default -> true;
        };
    }

    @OptionHook("profile.full-name")
    public void profileFullNameUpdated(OptionValueChange change) {
        universalContext.associate(change.getNodeId());
        if (!ObjectUtils.isEmpty(change.getNewValue())) {
            unpublishAndDelete(StoryType.REMINDER_FULL_NAME);
        }
    }

    @OptionHook("profile.avatar.id")
    public void profileAvatarUpdated(OptionValueChange change) {
        universalContext.associate(change.getNodeId());
        if (change.getNewValue() != null) {
            unpublishAndDelete(StoryType.REMINDER_AVATAR);
        }
    }

}
