package org.moera.node.auth.principal;

public interface AccessChecker {

    boolean isPrincipal(PrincipalFilter principal);

}
