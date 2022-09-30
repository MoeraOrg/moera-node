package org.moera.node.model;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReactionsFilter {

    private String ownerName;
    private Set<UUID> postings;

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Set<UUID> getPostings() {
        return postings;
    }

    public void setPostings(Set<UUID> postings) {
        this.postings = postings;
    }

}
