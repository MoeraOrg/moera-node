package org.moera.node.model;

import org.moera.lib.node.types.validate.ValidationException;

public class UnsubscribeFailure extends ValidationException {

    public UnsubscribeFailure() {
        super("subscription.unsubscribe");
    }

}
