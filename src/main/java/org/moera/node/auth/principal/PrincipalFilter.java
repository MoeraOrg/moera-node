package org.moera.node.auth.principal;

public interface PrincipalFilter {

    boolean includes(boolean admin, String nodeName);

}
