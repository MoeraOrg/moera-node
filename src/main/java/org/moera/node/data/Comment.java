package org.moera.node.data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("1")
public class Comment extends Entry {

    public Comment() {
        setEntryType(EntryType.COMMENT);
    }

    public Posting getPosting() {
        return (Posting) getParent();
    }

    public void setPosting(Posting posting) {
        setParent(posting);
    }

}
