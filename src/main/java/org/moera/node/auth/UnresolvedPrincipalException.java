package org.moera.node.auth;

public class UnresolvedPrincipalException extends RuntimeException {

    public UnresolvedPrincipalException(Principal principal) {
        super("Unresolved or unknown principal cannot be verified: " + principal);
    }

}
