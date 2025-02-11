package org.moera.node.auth.principal;

import org.moera.lib.node.types.Scope;

public interface AccessChecker {

    boolean isPrincipal(PrincipalFilter principal, Scope scope);

}
