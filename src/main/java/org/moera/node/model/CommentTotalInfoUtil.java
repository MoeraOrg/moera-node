package org.moera.node.model;

import org.moera.lib.node.types.CommentTotalInfo;

public class CommentTotalInfoUtil {

    public static CommentTotalInfo build(int total) {
        CommentTotalInfo info = new CommentTotalInfo();
        info.setTotal(total);
        return info;
    }

}
