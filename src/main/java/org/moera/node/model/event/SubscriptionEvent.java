package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.Subscription;
import org.moera.node.data.SubscriptionType;
import org.moera.node.model.AvatarImage;
import org.moera.node.util.Util;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionEvent extends Event {

    private String id;
    private SubscriptionType subscriptionType;
    private String feedName;
    private String remoteSubscriberId;
    private String remoteNodeName;
    private String remoteFullName;
    private AvatarImage remoteAvatar;
    private String remoteFeedName;
    private String remotePostingId;
    private Long createdAt;

    public SubscriptionEvent(EventType type) {
        super(type);
    }

    public SubscriptionEvent(EventType type, Subscription subscription) {
        super(type);
        id = subscription.getId().toString();
        subscriptionType = subscription.getSubscriptionType();
        feedName = subscription.getFeedName();
        remoteSubscriberId = subscription.getRemoteSubscriberId();
        remoteNodeName = subscription.getRemoteNodeName();
        remoteFullName = subscription.getRemoteFullName();
        if (subscription.getRemoteAvatarMediaFile() != null) {
            remoteAvatar = new AvatarImage(subscription.getRemoteAvatarMediaFile(), subscription.getRemoteAvatarShape());
        }
        remoteFeedName = subscription.getRemoteFeedName();
        remotePostingId = subscription.getRemoteEntryId();
        createdAt = Util.toEpochSecond(subscription.getCreatedAt());
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    public String getFeedName() {
        return feedName;
    }

    public void setFeedName(String feedName) {
        this.feedName = feedName;
    }

    public String getRemoteSubscriberId() {
        return remoteSubscriberId;
    }

    public void setRemoteSubscriberId(String remoteSubscriberId) {
        this.remoteSubscriberId = remoteSubscriberId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public AvatarImage getRemoteAvatar() {
        return remoteAvatar;
    }

    public void setRemoteAvatar(AvatarImage remoteAvatar) {
        this.remoteAvatar = remoteAvatar;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getRemotePostingId() {
        return remotePostingId;
    }

    public void setRemotePostingId(String remotePostingId) {
        this.remotePostingId = remotePostingId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("subscriptionType", LogUtil.format(subscriptionType.toString())));
        parameters.add(Pair.of("feedName", LogUtil.format(feedName)));
        parameters.add(Pair.of("remoteSubscriberId", LogUtil.format(remoteSubscriberId)));
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remoteFeedName", LogUtil.format(remoteFeedName)));
        parameters.add(Pair.of("remotePostingId", LogUtil.format(remotePostingId)));
    }

}
