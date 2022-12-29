package org.moera.node.auth.principal;

public interface PrincipalFilter {

    PrincipalExpression a();

    boolean includes(boolean admin, String nodeName, boolean subscribed, String[] friendGroups);

}
