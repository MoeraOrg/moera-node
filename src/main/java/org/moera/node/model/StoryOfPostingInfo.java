package org.moera.node.model;

import java.util.UUID;

import org.moera.node.data.Story;

public class StoryOfPostingInfo extends StoryInfo {

    private PostingInfo posting;

    public StoryOfPostingInfo() {
    }

    protected StoryOfPostingInfo(Story story, PostingInfo posting, boolean isAdmin) {
        super(story, isAdmin);
        this.posting = posting;
    }

    protected StoryOfPostingInfo(Story story, UUID postingId, boolean isAdmin) {
        this(story, new PostingInfo(postingId), isAdmin);
    }

    public PostingInfo getPosting() {
        return posting;
    }

    public void setPosting(PostingInfo posting) {
        this.posting = posting;
    }

}
