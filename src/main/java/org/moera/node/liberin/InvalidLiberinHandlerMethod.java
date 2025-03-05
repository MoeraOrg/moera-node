package org.moera.node.liberin;

import java.lang.reflect.Method;

public class InvalidLiberinHandlerMethod extends RuntimeException {

    public InvalidLiberinHandlerMethod(Method method) {
        super("%s method is not a valid liberin handler".formatted(method));
    }

}
