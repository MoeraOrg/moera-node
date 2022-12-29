package org.moera.node.auth.principal;

import java.util.HashMap;
import java.util.Map;

public class PrincipalFlag {

    public static final int NONE = 0x0001;
    public static final int ADMIN = 0x0002;
    public static final int SIGNED = 0x0004;
    public static final int PRIVATE = 0x0008;
    public static final int OWNER = 0x0010;
    public static final int PUBLIC = 0x0020;
    public static final int NODE = 0x0040;
    public static final int ONLY = 0x0080;
    public static final int UNSET = 0x0100;
    public static final int SECRET = 0x0200;
    public static final int SENIOR = 0x0400;
    public static final int ENIGMA = 0x0800;
    public static final int MAJOR = 0x1000;
    public static final int FRIENDS = 0x2000;
    public static final int SUBSCRIBED = 0x4000;

    public static final Map<String, Integer> NAMES = new HashMap<>();

    static {
        NAMES.put("none", NONE);
        NAMES.put("admin", ADMIN);
        NAMES.put("signed", SIGNED);
        NAMES.put("private", PRIVATE);
        NAMES.put("owner", OWNER);
        NAMES.put("public", PUBLIC);
        NAMES.put("node", NODE);
        NAMES.put("only", ONLY);
        NAMES.put("unset", UNSET);
        NAMES.put("secret", SECRET);
        NAMES.put("senior", SENIOR);
        NAMES.put("enigma", ENIGMA);
        NAMES.put("major", MAJOR);
        NAMES.put("friends", FRIENDS);
        NAMES.put("subscribed", SUBSCRIBED);
    }

    public static int fromName(String name) {
        Integer flag = NAMES.get(name);
        if (flag == null) {
            throw new IllegalArgumentException("Unknown principal flag name");
        }
        return flag;
    }

    public static int fromNames(String[] names) {
        int flags = 0;
        for (String name : names) {
            flags |= fromName(name);
        }
        return flags;
    }

}
