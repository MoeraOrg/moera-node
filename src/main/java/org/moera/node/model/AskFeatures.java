package org.moera.node.model;

import org.moera.node.auth.principal.Principal;
import org.moera.node.option.Options;

public class AskFeatures {

    private Principal subscribe;
    private Principal friend;

    public AskFeatures(Options options) {
        subscribe = options.getPrincipal("ask.subscribe.allowed");
        friend = options.getPrincipal("ask.friend.allowed");
    }

    public Principal getSubscribe() {
        return subscribe;
    }

    public void setSubscribe(Principal subscribe) {
        this.subscribe = subscribe;
    }

    public Principal getFriend() {
        return friend;
    }

    public void setFriend(Principal friend) {
        this.friend = friend;
    }

}
