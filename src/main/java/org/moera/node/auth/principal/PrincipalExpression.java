package org.moera.node.auth.principal;

public final class PrincipalExpression implements PrincipalFilter {

    enum Operation {
        OR, AND
    }

    private final Principal principal;
    private final boolean inverse;
    private final Operation operation;
    private final PrincipalExpression left;
    private final PrincipalExpression right;

    private PrincipalExpression(Principal principal, boolean inverse) {
        this.principal = principal;
        this.inverse = inverse;
        operation = null;
        left = null;
        right = null;
    }

    private PrincipalExpression(Operation operation, PrincipalExpression left, PrincipalExpression right) {
        principal = null;
        this.inverse = false;
        this.operation = operation;
        this.left = left;
        this.right = right;
    }

    private PrincipalExpression(Principal principal, boolean inverse, Operation operation,
                                PrincipalExpression left, PrincipalExpression right) {
        this.principal = principal;
        this.inverse = inverse;
        this.operation = operation;
        this.left = left;
        this.right = right;
    }

    @Override
    public PrincipalExpression a() {
        return this;
    }

    public static PrincipalExpression by(Principal principal) {
        return new PrincipalExpression(principal, false);
    }

    public static PrincipalExpression byNot(Principal principal) {
        return new PrincipalExpression(principal, true);
    }

    public static PrincipalExpression not(PrincipalExpression expression) {
        return new PrincipalExpression(expression.principal, !expression.inverse, expression.operation,
                expression.left, expression.right);
    }

    public PrincipalExpression or(PrincipalExpression expression) {
        return new PrincipalExpression(Operation.OR, this, expression);
    }

    public PrincipalExpression or(Principal principal) {
        return new PrincipalExpression(Operation.OR, this, by(principal));
    }

    public PrincipalExpression orNot(Principal principal) {
        return new PrincipalExpression(Operation.OR, this, byNot(principal));
    }

    public PrincipalExpression and(PrincipalExpression expression) {
        return new PrincipalExpression(Operation.AND, this, expression);
    }

    public PrincipalExpression and(Principal principal) {
        return new PrincipalExpression(Operation.AND, this, by(principal));
    }

    public PrincipalExpression andNot(Principal principal) {
        return new PrincipalExpression(Operation.AND, this, byNot(principal));
    }

    @Override
    public boolean includes(boolean admin, String nodeName, boolean subscribed, String[] friendGroups) {
        if (principal != null) {
            return inverse != principal.includes(admin, nodeName, subscribed, friendGroups);
        }
        if (operation == Operation.OR) {
            if (left == null) {
                throw new InvalidPrincipalExpression("Left operand not set");
            }
            if (right == null) {
                throw new InvalidPrincipalExpression("Right operand not set");
            }
            return inverse
                    != (left.includes(admin, nodeName, subscribed, friendGroups)
                        || right.includes(admin, nodeName, subscribed, friendGroups));
        }
        if (operation == Operation.AND) {
            if (left == null) {
                throw new InvalidPrincipalExpression("Left operand not set");
            }
            if (right == null) {
                throw new InvalidPrincipalExpression("Right operand not set");
            }
            return inverse
                    != (left.includes(admin, nodeName, subscribed, friendGroups)
                        && right.includes(admin, nodeName, subscribed, friendGroups));
        }
        throw new InvalidPrincipalExpression("Operation not set");
    }

    @Override
    public String toString() {
        if (principal != null) {
            return (inverse ? "not " : "") + principal;
        }
        if (operation == null) {
            throw new InvalidPrincipalExpression("Operation not set");
        }
        if (left == null) {
            throw new InvalidPrincipalExpression("Left operand not set");
        }
        if (right == null) {
            throw new InvalidPrincipalExpression("Right operand not set");
        }
        return inverse
                ? String.format("not ((%s) %s (%s))", left, operation.name().toLowerCase(), right)
                : String.format("(%s) %s (%s)", left, operation.name().toLowerCase(), right);
    }

}
