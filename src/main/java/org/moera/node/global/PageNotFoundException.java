package org.moera.node.global;

import org.moera.node.model.ObjectNotFoundFailure;

public class PageNotFoundException extends ObjectNotFoundFailure {

    public PageNotFoundException() {
        super("Page not found");
    }

}
