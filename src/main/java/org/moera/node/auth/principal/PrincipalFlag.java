package org.moera.node.auth.principal;

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

}
