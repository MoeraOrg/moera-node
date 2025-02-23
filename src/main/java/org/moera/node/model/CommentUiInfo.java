package org.moera.node.model;

import org.moera.lib.node.types.CommentInfo;

public class CommentUiInfo extends CommentInfo {

    public String getSaneBodyPreview() {
        return CommentInfoUtil.getSaneBodyPreview(this);
    }

    public String getSaneBody() {
        return CommentInfoUtil.getSaneBody(this);
    }

}
