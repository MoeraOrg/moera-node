package org.moera.node.auth.principal;

public class AccessCheckers {

    public static final AccessChecker PUBLIC = principal -> principal.includes(false, null);
    public static final AccessChecker ADMIN = principal -> principal.includes(true, null);

    public static AccessChecker node(String nodeName) {
        return principal -> principal.includes(false, nodeName);
    }

}
