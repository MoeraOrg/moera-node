package org.moera.node.rest.notification;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.moera.node.data.SubscriptionReason;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.model.MentionInRemotePostingAddedLiberin;
import org.moera.node.liberin.model.MentionInRemotePostingDeletedLiberin;
import org.moera.node.media.MediaManager;
import org.moera.node.model.AvatarImage;
import org.moera.node.model.notification.MentionPostingAddedNotification;
import org.moera.node.model.notification.MentionPostingDeletedNotification;
import org.moera.node.model.notification.NotificationType;
import org.moera.node.notification.receive.NotificationMapping;
import org.moera.node.notification.receive.NotificationProcessor;
import org.moera.node.rest.task.RemotePostingCommentsSubscribeTask;
import org.moera.node.task.TaskAutowire;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

@NotificationProcessor
public class MentionPostingProcessor {

    @Inject
    private UniversalContext universalContext;

    @Inject
    private TaskAutowire taskAutowire;

    @Inject
    @Qualifier("remoteTaskExecutor")
    private TaskExecutor taskExecutor;

    @Inject
    private MediaManager mediaManager;

    @NotificationMapping(NotificationType.MENTION_POSTING_ADDED)
    @Transactional
    public void added(MentionPostingAddedNotification notification) {
        mediaManager.asyncDownloadPublicMedia(notification.getSenderNodeName(),
                new AvatarImage[] {notification.getOwnerAvatar()},
                mediaFiles -> {
                    if (notification.getOwnerAvatar() != null) {
                        notification.getOwnerAvatar().setMediaFile(mediaFiles[0]);
                    }
                    universalContext.send(
                            new MentionInRemotePostingAddedLiberin(notification.getSenderNodeName(),
                                    notification.getOwnerName(), notification.getOwnerFullName(),
                                    notification.getOwnerGender(), notification.getOwnerAvatar(),
                                    notification.getPostingId(), notification.getHeading()));
                });
        var task = new RemotePostingCommentsSubscribeTask(
                notification.getSenderNodeName(), notification.getPostingId(), SubscriptionReason.MENTION);
        taskAutowire.autowire(task);
        taskExecutor.execute(task);
    }

    @NotificationMapping(NotificationType.MENTION_POSTING_DELETED)
    @Transactional
    public void deleted(MentionPostingDeletedNotification notification) {
        universalContext.send(
                new MentionInRemotePostingDeletedLiberin(notification.getSenderNodeName(), notification.getPostingId()));
    }

}
