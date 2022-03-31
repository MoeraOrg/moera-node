package org.moera.node.liberin.model;

import java.util.List;

import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.StoryAttributes;

public class PostingAddedLiberin extends Liberin {

    private Posting posting;
    private List<StoryAttributes> publications;

    public PostingAddedLiberin(Posting posting, List<StoryAttributes> publications) {
        this.posting = posting;
        this.publications = publications;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public List<StoryAttributes> getPublications() {
        return publications;
    }

    public void setPublications(List<StoryAttributes> publications) {
        this.publications = publications;
    }

}
