package org.moera.node.model;

import java.util.Collections;

import org.moera.lib.node.types.ReactionsSliceInfo;
import org.moera.node.util.SafeInteger;

public class ReactionsSliceInfoUtil {

    public static final ReactionsSliceInfo EMPTY = new ReactionsSliceInfo();

    static {
        EMPTY.setBefore(SafeInteger.MAX_VALUE);
        EMPTY.setAfter(SafeInteger.MIN_VALUE);
        EMPTY.setTotal(0);
        EMPTY.setReactions(Collections.emptyList());
    }

}
