package org.moera.node.model.notification;

import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.commons.util.LogUtil;
import org.springframework.data.util.Pair;

public abstract class SheriffOrderForPostingNotification extends Notification {

    @NotBlank
    @Size(max = 63)
    private String remoteNodeName;

    @NotBlank
    @Size(max = 63)
    private String remoteFeedName;

    @Size(max = 255)
    private String postingHeading;

    @NotBlank
    @Size(max = 40)
    private String postingId;

    @NotBlank
    @Size(max = 40)
    private String orderId;

    public SheriffOrderForPostingNotification(NotificationType type) {
        super(type);
    }

    public SheriffOrderForPostingNotification(NotificationType type, String remoteNodeName, String remoteFeedName,
                                              String postingHeading, String postingId, String orderId) {
        super(type);
        this.remoteNodeName = remoteNodeName;
        this.remoteFeedName = remoteFeedName;
        this.postingHeading = postingHeading;
        this.postingId = postingId;
        this.orderId = orderId;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFeedName() {
        return remoteFeedName;
    }

    public void setRemoteFeedName(String remoteFeedName) {
        this.remoteFeedName = remoteFeedName;
    }

    public String getPostingHeading() {
        return postingHeading;
    }

    public void setPostingHeading(String postingHeading) {
        this.postingHeading = postingHeading;
    }

    public String getPostingId() {
        return postingId;
    }

    public void setPostingId(String postingId) {
        this.postingId = postingId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("remoteNodeName", LogUtil.format(remoteNodeName)));
        parameters.add(Pair.of("remoteFeedName", LogUtil.format(remoteFeedName)));
        parameters.add(Pair.of("postingHeading", LogUtil.format(postingHeading)));
        parameters.add(Pair.of("postingId", LogUtil.format(postingId)));
        parameters.add(Pair.of("orderId", LogUtil.format(orderId)));
    }

}
