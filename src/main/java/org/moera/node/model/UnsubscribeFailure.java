package org.moera.node.model;

import org.moera.lib.node.types.validate.ValidationFailure;

public class UnsubscribeFailure extends ValidationFailure {

    public UnsubscribeFailure() {
        super("subscription.unsubscribe");
    }

}
