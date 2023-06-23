package org.moera.node.data;

import java.util.Objects;

public class UserList {

    public static final String SHERIFF_HIDE = "sheriff-hide";

    public static boolean isKnown(String listName) {
        return Objects.equals(listName, SHERIFF_HIDE);
    }

}
