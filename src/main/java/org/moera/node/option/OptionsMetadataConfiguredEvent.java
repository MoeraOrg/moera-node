package org.moera.node.option;

import org.springframework.context.ApplicationEvent;

public class OptionsMetadataConfiguredEvent extends ApplicationEvent {

    public OptionsMetadataConfiguredEvent(Object source) {
        super(source);
    }

}
