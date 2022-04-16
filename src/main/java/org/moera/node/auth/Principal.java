package org.moera.node.auth;

import java.util.Objects;

public class Principal {

    public static final Principal NONE = new Principal("none");
    public static final Principal ADMIN = new Principal("admin");
    public static final Principal SIGNED = new Principal("signed");
    public static final Principal RULER = new Principal("ruler");
    public static final Principal OWNER = new Principal("owner");
    public static final Principal PUBLIC = new Principal("public");

    private final String value;

    Principal(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Principal cannot be null");
        }

        this.value = value;
    }

    public static Principal ofNode(String nodeName) {
        return new Principal("node:" + nodeName);
    }

    public static Principal ofOnly(String nodeName) {
        return new Principal("only:" + nodeName);
    }

    public String getValue() {
        return value;
    }

    public boolean isNone() {
        return value.equals(NONE.value);
    }

    public boolean isAdmin() {
        return value.equals(ADMIN.value);
    }

    public boolean isSigned() {
        return value.equals(SIGNED.value);
    }

    public boolean isRuler() {
        return value.equals(RULER.value);
    }

    public boolean isOwner() {
        return value.equals(OWNER.value);
    }

    public boolean isPublic() {
        return value.equals(PUBLIC.value);
    }

    public boolean isNode() {
        return value.startsWith("node:");
    }

    public boolean isOnly() {
        return value.startsWith("only:");
    }

    public String getNodeName() {
        return isNode() || isOnly() ? value.substring(5) : null;
    }

    public Principal withOwner(String ownerName) {
        if (isOwner()) {
            return Principal.ofOnly(ownerName);
        }
        if (isRuler()) {
            return Principal.ofNode(ownerName);
        }
        return this;
    }

    @Override
    public boolean equals(Object peer) {
        if (this == peer) {
            return true;
        }
        if (peer == null || getClass() != peer.getClass()) {
            return false;
        }

        Principal principal = (Principal) peer;
        return value.equals(principal.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }

}
