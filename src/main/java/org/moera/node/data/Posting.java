package org.moera.node.data;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import org.moera.node.option.Options;

@Entity
@DiscriminatorValue("0")
public class Posting extends Entry {

    public Posting() {
        setEntryType(EntryType.POSTING);
    }

    public String getAcceptedReactionsPositiveOrDefault(Options options) {
        return getAcceptedReactionsPositive() != null
                ? getAcceptedReactionsPositive()
                : options.getString("posting.reactions.positive.accepted");
    }

    public String getAcceptedReactionsNegativeOrDefault(Options options) {
        return getAcceptedReactionsNegative() != null
                ? getAcceptedReactionsNegative()
                : options.getString("posting.reactions.negative.accepted");
    }

}
