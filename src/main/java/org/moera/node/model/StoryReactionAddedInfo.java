package org.moera.node.model;

import java.util.UUID;

import org.moera.node.data.Story;

public class StoryReactionAddedInfo extends StoryInfo {

    private PostingInfo posting;

    public StoryReactionAddedInfo() {
    }

    protected StoryReactionAddedInfo(Story story, UUID postingId, boolean isAdmin) {
        super(story, isAdmin);
        this.posting = new PostingInfo(postingId);
    }

    public PostingInfo getPosting() {
        return posting;
    }

    public void setPosting(PostingInfo posting) {
        this.posting = posting;
    }

}
