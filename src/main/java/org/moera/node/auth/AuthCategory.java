package org.moera.node.auth;

import java.util.ArrayList;
import java.util.List;

public class AuthCategory {

    public static final long ALL = 0;

    public static final long OTHER = 0x0001;
    public static final long VIEW_MEDIA = 0x0002;

    public static String[] toStrings(long authCategory) {
        List<String> categories = new ArrayList<>();
        if ((authCategory & OTHER) != 0 || authCategory == ALL) {
            categories.add("other");
        }
        if ((authCategory & VIEW_MEDIA) != 0 || authCategory == ALL) {
            categories.add("view-media");
        }
        return categories.toArray(String[]::new);
    }

}
