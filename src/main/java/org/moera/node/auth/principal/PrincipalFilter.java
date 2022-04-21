package org.moera.node.auth.principal;

public final class PrincipalFilter {

    private final Principal principal;
    private final boolean inverse;

    private PrincipalFilter(Principal principal, boolean inverse) {
        this.principal = principal;
        this.inverse = inverse;
    }

    public static PrincipalFilter by(Principal principal) {
        return new PrincipalFilter(principal, false);
    }

    public static PrincipalFilter byNot(Principal principal) {
        return new PrincipalFilter(principal, true);
    }

    public boolean includes(boolean admin, String nodeName) {
        return inverse != principal.includes(admin, nodeName);
    }

}
