package org.moera.node.domain;

import org.springframework.context.ApplicationEvent;

public class DomainsConfiguredEvent extends ApplicationEvent {

    public DomainsConfiguredEvent(Object source) {
        super(source);
    }

}
