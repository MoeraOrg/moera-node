package org.moera.node.userlist;

import java.util.Objects;

public class UserList {

    public static final String SHERIFF_HIDE = "sheriff-hide";

    public static boolean isKnown(String listName) {
        return Objects.equals(listName, SHERIFF_HIDE);
    }

}
