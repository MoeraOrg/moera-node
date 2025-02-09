package org.moera.node.data;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("1")
public class Comment extends Entry {

    public Comment() {
        setEntryType(EntryType.COMMENT);
    }

    public Entry getPosting() {
        return getParent();
    }

    public void setPosting(Posting posting) {
        setParent(posting);
    }

}
