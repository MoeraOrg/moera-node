package org.moera.node.model;

import org.moera.node.data.Story;

public class StoryPostingAddedInfo extends StoryInfo {

    private PostingInfo posting;

    public StoryPostingAddedInfo() {
    }

    protected StoryPostingAddedInfo(Story story, PostingInfo posting, boolean isAdmin) {
        super(story, isAdmin);
        this.posting = posting;
    }

    public PostingInfo getPosting() {
        return posting;
    }

    public void setPosting(PostingInfo posting) {
        this.posting = posting;
    }

}
