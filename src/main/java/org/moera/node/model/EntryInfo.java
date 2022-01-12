package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class EntryInfo {

    private PostingInfo posting;
    private CommentInfo comment;

    public EntryInfo() {
    }

    public EntryInfo(PostingInfo posting) {
        this.posting = posting;
    }

    public EntryInfo(CommentInfo comment) {
        this.comment = comment;
    }

    public PostingInfo getPosting() {
        return posting;
    }

    public void setPosting(PostingInfo posting) {
        this.posting = posting;
    }

    public CommentInfo getComment() {
        return comment;
    }

    public void setComment(CommentInfo comment) {
        this.comment = comment;
    }

}
