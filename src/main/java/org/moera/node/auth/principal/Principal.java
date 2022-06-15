package org.moera.node.auth.principal;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

@JsonSerialize(converter = Principal.ToStringConverter.class)
@JsonDeserialize(converter = Principal.FromStringConverter.class)
public class Principal implements Cloneable, PrincipalFilter {

    /*
     *   admin   object
     *     +       +      private and wider
     *     +       +      secret
     *     +       -      senior
     *     +       +      enigma
     *     +       -      major
     *     -       +      owner
     *     +       -      admin
     *
     *   admin   post   object
     *     +      +       +      private and wider
     *     +      -       +      secret
     *     +      +       -      senior
     *     +      -       +      enigma
     *     +      -       -      major
     *     -      -       +      owner
     *     +      -       -      admin
     *
     *   admin   post   comment   object
     *     +      +        +        +      private and wider
     *     +      +        -        +      secret
     *     +      +        +        -      senior
     *     +      -        -        +      enigma
     *     +      +        -        -      major
     *     -      -        -        +      owner
     *     +      -        -        -      admin
     */
    public static final Principal NONE = new Principal("none");
    public static final Principal ADMIN = new Principal("admin");
    public static final Principal SIGNED = new Principal("signed");
    public static final Principal PRIVATE = new Principal("private");
    public static final Principal SECRET = new Principal("secret");
    public static final Principal SENIOR = new Principal("senior");
    public static final Principal ENIGMA = new Principal("enigma");
    public static final Principal MAJOR = new Principal("major");
    public static final Principal OWNER = new Principal("owner");
    public static final Principal PUBLIC = new Principal("public");
    public static final Principal UNSET = new Principal("unset");

    private static final Map<Principal, Integer> PRINCIPAL_MASKS = Map.of(
            Principal.PRIVATE, 0xf,
            Principal.SECRET, 0xd,
            Principal.SENIOR, 0xe,
            Principal.ENIGMA, 0x9,
            Principal.MAJOR, 0xc,
            Principal.OWNER, 0x1,
            Principal.ADMIN, 0x8,
            Principal.NONE, 0x0
    );

    private final String value;

    Principal(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Principal cannot be null");
        }

        this.value = value;
    }

    public static Principal ofNode(String... nodeNames) {
        return new Principal("node:" + joinNotNull(",", nodeNames));
    }

    public static Principal ofOnly(String... nodeNames) {
        return new Principal("only:" + joinNotNull(",", nodeNames));
    }

    private static String joinNotNull(String delimiter, String[] names) {
        StringBuilder result = new StringBuilder();
        for (String name : names) {
            if (name != null) {
                if (result.length() > 0) {
                    result.append(delimiter);
                }
                result.append(name);
            }
        }
        return result.toString();
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

    public boolean isSecret() {
        return value.equals(SECRET.value);
    }

    public boolean isSenior() {
        return value.equals(SENIOR.value);
    }

    public boolean isEnigma() {
        return value.equals(ENIGMA.value);
    }

    public boolean isMajor() {
        return value.equals(MAJOR.value);
    }

    public boolean isOwner() {
        return value.equals(OWNER.value);
    }

    public boolean isPublic() {
        return value.equals(PUBLIC.value);
    }

    public boolean isUnset() {
        return value.equals(UNSET.value);
    }

    public boolean isNode() {
        return value.startsWith("node:");
    }

    public boolean isOnly() {
        return value.startsWith("only:");
    }

    public String[] getNodeNames() {
        return isNode() || isOnly() ? value.substring(5).split(",") : new String[0];
    }

    public Principal withOwner(String ownerName) {
        if (isOwner()) {
            return Principal.ofOnly(ownerName);
        }
        if (isPrivate() | isSecret() | isEnigma()) {
            return Principal.ofNode(ownerName);
        }
        if (isSenior() | isMajor()) {
            return Principal.ADMIN;
        }
        return this;
    }

    public Principal withOwner(String ownerName, String seniorName) {
        if (isOwner()) {
            return Principal.ofOnly(ownerName);
        }
        if (isPrivate()) {
            return Principal.ofNode(ownerName, seniorName);
        }
        if (isSecret() | isEnigma()) {
            return Principal.ofNode(ownerName);
        }
        if (isSenior()) {
            return Principal.ofNode(seniorName);
        }
        if (isMajor()) {
            return Principal.ADMIN;
        }
        return this;
    }

    public Principal withOwner(String ownerName, String seniorName, String majorName) {
        if (isOwner()) {
            return Principal.ofOnly(ownerName);
        }
        if (isPrivate()) {
            return Principal.ofNode(ownerName, seniorName, majorName);
        }
        if (isSecret()) {
            return Principal.ofNode(ownerName, majorName);
        }
        if (isSenior()) {
            return Principal.ofNode(seniorName, majorName);
        }
        if (isEnigma()) {
            return Principal.ofNode(ownerName);
        }
        if (isMajor()) {
            return Principal.ofNode(majorName);
        }
        return this;
    }

    public Principal withSubordinate(Principal principal) {
        return isUnset() ? principal : this;
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
            return includes(getNodeNames(), nodeName);
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
            return includes(getNodeNames(), nodeName);
        }
        throw new UnresolvedPrincipalException(this);
    }

    private static boolean includes(String[] names, String name) {
        if (names == null || names.length == 0 || name == null) {
            return false;
        }
        for (String s : names) {
            if (Objects.equals(s, name)) {
                return true;
            }
        }
        return false;
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
        if (isSecret()) {
            return (flags & PrincipalFlag.SECRET) != 0;
        }
        if (isSenior()) {
            return (flags & PrincipalFlag.SENIOR) != 0;
        }
        if (isEnigma()) {
            return (flags & PrincipalFlag.ENIGMA) != 0;
        }
        if (isMajor()) {
            return (flags & PrincipalFlag.MAJOR) != 0;
        }
        if (isOwner()) {
            return (flags & PrincipalFlag.OWNER) != 0;
        }
        if (isPublic()) {
            return (flags & PrincipalFlag.PUBLIC) != 0;
        }
        if (isUnset()) {
            return (flags & PrincipalFlag.UNSET) != 0;
        }
        if (isNode()) {
            return (flags & PrincipalFlag.NODE) != 0;
        }
        if (isOnly()) {
            return (flags & PrincipalFlag.ONLY) != 0;
        }
        return false;
    }

    public Principal union(Principal principal) {
        if (isUnset()) {
            return principal;
        }
        if (isPublic() || principal.isPublic()) {
            return Principal.PUBLIC;
        }
        if (isSigned() || principal.isSigned()) {
            return Principal.SIGNED;
        }

        Integer mask = PRINCIPAL_MASKS.get(this);
        Integer principalMask = PRINCIPAL_MASKS.get(principal);
        return mask != null && principalMask != null ? closeToMask(mask | principalMask) : Principal.NONE;
    }

    public Principal intersect(Principal principal) {
        if (isUnset()) {
            return principal;
        }
        if (isPublic()) {
            return principal;
        }
        if (principal.isPublic()) {
            return this;
        }
        if (isSigned()) {
            return principal;
        }
        if (principal.isSigned()) {
            return this;
        }

        Integer mask = PRINCIPAL_MASKS.get(this);
        Integer principalMask = PRINCIPAL_MASKS.get(principal);
        return mask != null && principalMask != null ? closeToMask(mask & principalMask) : Principal.NONE;
    }

    private Principal closeToMask(int mask) {
        Principal closest = null;
        int closeness = 256;
        for (var entry : PRINCIPAL_MASKS.entrySet()) {
            int c = Integer.bitCount(entry.getValue() ^ mask);
            if (c < closeness) {
                closest = entry.getKey();
                closeness = c;
            }
        }
        return closest;
    }

    @Override
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
