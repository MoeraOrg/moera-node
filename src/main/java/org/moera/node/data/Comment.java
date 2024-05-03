package org.moera.node.data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
