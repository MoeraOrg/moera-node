package org.moera.node.auth.principal;

public class AccessCheckers {

    public static final AccessChecker PUBLIC = (principal, scope) -> principal.includes(false, null, false, null);
    public static final AccessChecker ADMIN = (principal, scope) -> principal.includes(true, null, false, null);

    public static AccessChecker node(String nodeName) {
        return (principal, scope) -> principal.includes(false, nodeName, false, null);
        // FIXME subscription status and friend groups should correspond the node
    }

}
