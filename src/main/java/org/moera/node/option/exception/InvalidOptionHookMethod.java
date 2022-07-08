package org.moera.node.option.exception;

import java.lang.reflect.Method;

public class InvalidOptionHookMethod extends RuntimeException {

    public InvalidOptionHookMethod(Method method) {
        super(String.format("%s method is not a valid option hook method", method));
    }

}
