package org.moera.node.model;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionFilter {

    private Set<RemotePosting> postings;

    public Set<RemotePosting> getPostings() {
        return postings;
    }

    public void setPostings(Set<RemotePosting> postings) {
        this.postings = postings;
    }

}
