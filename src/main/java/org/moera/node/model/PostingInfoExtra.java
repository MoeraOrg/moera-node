package org.moera.node.model;

public class PostingInfoExtra {

    private String saneBodyPreview;
    private String saneBody;
    private boolean sheriffUserListReferred;

    public String getSaneBodyPreview() {
        return saneBodyPreview;
    }

    public void setSaneBodyPreview(String saneBodyPreview) {
        this.saneBodyPreview = saneBodyPreview;
    }

    public String getSaneBody() {
        return saneBody;
    }

    public void setSaneBody(String saneBody) {
        this.saneBody = saneBody;
    }

    public boolean isSheriffUserListReferred() {
        return sheriffUserListReferred;
    }

    public void setSheriffUserListReferred(boolean sheriffUserListReferred) {
        this.sheriffUserListReferred = sheriffUserListReferred;
    }

}
