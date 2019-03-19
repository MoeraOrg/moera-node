package org.moera.node.option;

import org.springframework.context.ApplicationEvent;

public class OptionsLoadedEvent extends ApplicationEvent {

    public OptionsLoadedEvent(Object source) {
        super(source);
    }

}
