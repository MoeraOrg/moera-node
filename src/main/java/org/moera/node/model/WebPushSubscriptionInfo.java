package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.WebPushSubscription;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WebPushSubscriptionInfo {

    private String id;

    public WebPushSubscriptionInfo() {
    }

    public WebPushSubscriptionInfo(WebPushSubscription subscription) {
        this.id = subscription.getId().toString();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
