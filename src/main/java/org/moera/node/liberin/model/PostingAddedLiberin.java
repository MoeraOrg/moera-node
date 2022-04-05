package org.moera.node.liberin.model;

import java.util.List;

import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class PostingAddedLiberin extends Liberin {

    private Posting posting;
    private List<String> feeds;

    public PostingAddedLiberin(Posting posting, List<String> feeds) {
        this.posting = posting;
        this.feeds = feeds;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public List<String> getFeeds() {
        return feeds;
    }

    public void setFeeds(List<String> feeds) {
        this.feeds = feeds;
    }

}
