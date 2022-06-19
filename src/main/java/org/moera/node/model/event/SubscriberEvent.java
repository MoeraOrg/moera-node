package org.moera.node.model.event;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.PrincipalFilter;
import org.moera.node.data.Subscriber;
import org.moera.node.data.SubscriptionType;
import org.moera.node.model.AvatarImage;
import org.moera.node.util.Util;
import org.springframework.data.util.Pair;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriberEvent extends Event {

    private String id;
    private SubscriptionType subscriptionType;
    private String feedName;
    private String postingId;
    private String nodeName;
    private String fullName;
    private AvatarImage avatar;
    private Long createdAt;

    public SubscriberEvent(EventType type) {
        super(type);
    }

    public SubscriberEvent(EventType type, Subscriber subscriber, PrincipalFilter filter) {
        super(type, filter);

        id = subscriber.getId().toString();
        subscriptionType = subscriber.getSubscriptionType();
        feedName = subscriber.getFeedName();
        if (subscriber.getEntry() != null) {
            postingId = subscriber.getEntry().getId().toString();
        }
        nodeName = subscriber.getRemoteNodeName();
        fullName = subscriber.getRemoteFullName();
        if (subscriber.getRemoteAvatarMediaFile() != null) {
            avatar = new AvatarImage(subscriber.getRemoteAvatarMediaFile(), subscriber.getRemoteAvatarShape());
        }
        createdAt = Util.toEpochSecond(subscriber.getCreatedAt());
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

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
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
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("nodeName", LogUtil.format(nodeName)));
    }

}
