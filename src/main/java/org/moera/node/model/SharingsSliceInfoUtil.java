package org.moera.node.model;

import java.util.Collections;

import org.moera.lib.node.types.SharingsSliceInfo;
import org.moera.node.util.SafeInteger;

public class SharingsSliceInfoUtil {

    public static final SharingsSliceInfo EMPTY = new SharingsSliceInfo();

    static {
        EMPTY.setBefore(SafeInteger.MAX_VALUE);
        EMPTY.setAfter(SafeInteger.MIN_VALUE);
        EMPTY.setTotal(0);
        EMPTY.setSharings(Collections.emptyList());
    }

}
