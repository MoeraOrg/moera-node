package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.WebPushSubscription;

public class WebPushSubscriptionAttributes {

    @NotBlank
    @Size(max = 255)
    private String endpoint;

    @NotBlank
    @Size(max = 128)
    private String publicKey;

    @NotBlank
    @Size(max = 32)
    private String authKey;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public void toWebPushSubscription(WebPushSubscription webPushSubscription) {
        webPushSubscription.setEndpoint(endpoint);
        webPushSubscription.setPublicKey(publicKey);
        webPushSubscription.setAuthKey(authKey);
    }

}
