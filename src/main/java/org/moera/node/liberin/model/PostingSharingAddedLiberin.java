package org.moera.node.liberin.model;

import org.moera.node.data.EntrySharing;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class PostingSharingAddedLiberin extends Liberin {

    private Posting posting;
    private EntrySharing sharing;

    public PostingSharingAddedLiberin(Posting posting, EntrySharing sharing) {
        this.posting = posting;
        this.sharing = sharing;
    }

    public Posting getPosting() {
        return posting;
    }

    public void setPosting(Posting posting) {
        this.posting = posting;
    }

    public EntrySharing getSharing() {
        return sharing;
    }

    public void setSharing(EntrySharing sharing) {
        this.sharing = sharing;
    }

}
