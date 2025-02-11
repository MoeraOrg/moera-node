package org.moera.node.model;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.lib.node.types.BlockedOperation;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryBlocked {

    private Set<BlockedOperation> operations = new HashSet<>();
    private Long period;

    public StorySummaryBlocked() {
    }

    public StorySummaryBlocked(Set<BlockedOperation> operations, Long period) {
        this.operations = operations;
        this.period = period;
    }

    public Set<BlockedOperation> getOperations() {
        return operations;
    }

    public void setOperations(Set<BlockedOperation> operations) {
        this.operations = operations;
    }

    public Long getPeriod() {
        return period;
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

}
