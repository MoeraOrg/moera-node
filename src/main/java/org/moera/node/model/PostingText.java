package org.moera.node.model;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

import org.moera.node.data.Posting;

public class PostingText {

    @NotBlank
    @Size(max = 65535)
    private String bodySrc;

    @NotBlank
    @Size(max = 65535)
    private String bodyHtml;

    public PostingText() {
    }

    public String getBodySrc() {
        return bodySrc;
    }

    public void setBodySrc(String bodySrc) {
        this.bodySrc = bodySrc;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public void toPosting(Posting posting) {
        posting.setBodySrc(bodySrc);
        posting.setBodyHtml(bodyHtml);
    }

}
