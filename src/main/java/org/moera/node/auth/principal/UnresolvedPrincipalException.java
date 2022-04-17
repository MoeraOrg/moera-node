package org.moera.node.auth.principal;

public class UnresolvedPrincipalException extends RuntimeException {

    public UnresolvedPrincipalException(Principal principal) {
        super("Relative or unknown principal cannot be verified: " + principal);
    }

}
