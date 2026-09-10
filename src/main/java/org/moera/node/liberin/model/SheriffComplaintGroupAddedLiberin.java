package org.moera.node.liberin.model;

import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.liberin.Liberin;

public class SheriffComplaintGroupAddedLiberin extends Liberin {

    private SheriffComplaintGroup group;

    public SheriffComplaintGroupAddedLiberin(SheriffComplaintGroup group) {
        this.group = group;
    }

    public SheriffComplaintGroup getGroup() {
        return group;
    }

    public void setGroup(SheriffComplaintGroup group) {
        this.group = group;
    }

}
