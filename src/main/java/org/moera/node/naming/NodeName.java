package org.moera.node.naming;

public interface NodeName {

    static NodeName parse(String name) {
        return RegisteredName.parse(name);
    }

}
