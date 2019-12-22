package org.moera.node.data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("0")
public class Posting extends Entry {

    public Posting() {
        setEntryType(EntryType.POSTING);
    }

}
