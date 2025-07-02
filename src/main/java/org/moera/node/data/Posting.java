package org.moera.node.data;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("0")
public class Posting extends Entry {

    public Posting() {
        setEntryType(EntryType.POSTING);
    }

}
