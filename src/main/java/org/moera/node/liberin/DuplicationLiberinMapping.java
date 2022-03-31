package org.moera.node.liberin;

import java.lang.reflect.Method;

public class DuplicationLiberinMapping extends RuntimeException {

    public DuplicationLiberinMapping(Class<? extends Liberin> type, Method method) {
        super(String.format("Liberin mapping for type %s is already declared on method %s",
                type.getSimpleName(), method));
    }

}
