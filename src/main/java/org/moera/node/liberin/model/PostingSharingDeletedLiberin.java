package org.moera.node.liberin.model;

import java.util.List;
import java.util.UUID;

import org.moera.node.data.EntrySharing;
import org.moera.node.data.Posting;
import org.moera.node.liberin.Liberin;

public class PostingSharingDeletedLiberin extends Liberin {

    private Posting posting;
    private EntrySharing sharing;
    private List<UUID> substoryIds;

    public PostingSharingDeletedLiberin(Posting posting, EntrySharing sharing, List<UUID> substoryIds) {
        this.posting = posting;
        this.sharing = sharing;
        this.substoryIds = substoryIds;
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

    public List<UUID> getSubstoryIds() {
        return substoryIds;
    }

    public void setSubstoryIds(List<UUID> substoryIds) {
        this.substoryIds = substoryIds;
    }

}
