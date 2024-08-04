package org.moera.node.auth.principal;

import org.moera.node.auth.Scope;

public interface AccessChecker {

    boolean isPrincipal(PrincipalFilter principal, Scope scope);

}
