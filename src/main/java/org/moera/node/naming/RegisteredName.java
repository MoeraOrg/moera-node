package org.moera.node.naming;

public interface RegisteredName {

    static RegisteredName parse(String name) {
        return DelegatedName.parse(name);
    }

    String format();

}
