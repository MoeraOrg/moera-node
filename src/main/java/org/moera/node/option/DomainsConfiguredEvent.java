package org.moera.node.option;

import org.springframework.context.ApplicationEvent;

public class DomainsConfiguredEvent extends ApplicationEvent {

    public DomainsConfiguredEvent(Object source) {
        super(source);
    }

}
