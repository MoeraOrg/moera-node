package org.moera.node.model;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionTotalsFilter {

    private Set<UUID> postings;

    public Set<UUID> getPostings() {
        return postings;
    }

    public void setPostings(Set<UUID> postings) {
        this.postings = postings;
    }

}
