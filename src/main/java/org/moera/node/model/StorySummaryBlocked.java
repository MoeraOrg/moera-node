package org.moera.node.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.BlockedOperation;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryBlocked {

    private Set<BlockedOperation> operations = new HashSet<>();

    public StorySummaryBlocked() {
    }

    public StorySummaryBlocked(Set<BlockedOperation> operations) {
        this.operations = operations;
    }

    public Set<BlockedOperation> getOperations() {
        return operations;
    }

    public void setOperations(Set<BlockedOperation> operations) {
        this.operations = operations;
    }

}
