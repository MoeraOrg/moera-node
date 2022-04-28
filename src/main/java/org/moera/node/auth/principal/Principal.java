package org.moera.node.auth.principal;

import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

@JsonSerialize(converter = Principal.ToStringConverter.class)
@JsonDeserialize(converter = Principal.FromStringConverter.class)
public class Principal implements Cloneable, PrincipalFilter {

    public static final Principal NONE = new Principal("none");
    public static final Principal ADMIN = new Principal("admin");
    public static final Principal SIGNED = new Principal("signed");
    public static final Principal PRIVATE = new Principal("private");
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

    public boolean isPrivate() {
        return value.equals(PRIVATE.value);
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
        if (isPrivate()) {
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

    @Override
    protected Principal clone() {
        return new Principal(value);
    }

    @Override
    public boolean includes(boolean admin, String nodeName) {
        if (isPublic()) {
            return true;
        }
        if (isNone()) {
            return false;
        }
        if (isOnly()) {
            return Objects.equals(nodeName, getNodeName());
        }
        if (admin) {
            return true;
        }
        if (isAdmin()) {
            return false;
        }
        if (isSigned()) {
            return nodeName != null;
        }
        if (isNode()) {
            return Objects.equals(nodeName, getNodeName());
        }
        throw new UnresolvedPrincipalException(this);
    }

    public boolean isOneOf(int flags) {
        if (isNone()) {
            return (flags & PrincipalFlag.NONE) != 0;
        }
        if (isAdmin()) {
            return (flags & PrincipalFlag.ADMIN) != 0;
        }
        if (isSigned()) {
            return (flags & PrincipalFlag.SIGNED) != 0;
        }
        if (isPrivate()) {
            return (flags & PrincipalFlag.PRIVATE) != 0;
        }
        if (isOwner()) {
            return (flags & PrincipalFlag.OWNER) != 0;
        }
        if (isPublic()) {
            return (flags & PrincipalFlag.PUBLIC) != 0;
        }
        if (isNode()) {
            return (flags & PrincipalFlag.NODE) != 0;
        }
        if (isOnly()) {
            return (flags & PrincipalFlag.ONLY) != 0;
        }
        return false;
    }

    public Principal or(Principal principal) {
        if (isPublic() || principal.isPublic()) {
            return Principal.PUBLIC;
        }
        if (isSigned() || principal.isSigned()) {
            return Principal.SIGNED;
        }
        if (isPrivate() || principal.isPrivate()) {
            return Principal.PRIVATE;
        }
        if (isAdmin() && principal.isOwner() || isOwner() && principal.isAdmin()) {
            return Principal.PRIVATE;
        }
        if (isOwner() || principal.isOwner()) {
            return Principal.OWNER;
        }
        if (isAdmin() || principal.isAdmin()) {
            return Principal.ADMIN;
        }
        return Principal.NONE;
    }

    public PrincipalExpression a() {
        return PrincipalExpression.by(this);
    }

    public PrincipalExpression not() {
        return PrincipalExpression.byNot(this);
    }

    public static class ToStringConverter extends StdConverter<Principal, String> {

        @Override
        public String convert(Principal principal) {
            return principal.getValue();
        }

    }

    public static class FromStringConverter extends StdConverter<String, Principal> {

        @Override
        public Principal convert(String s) {
            return new Principal(s);
        }

    }

}
