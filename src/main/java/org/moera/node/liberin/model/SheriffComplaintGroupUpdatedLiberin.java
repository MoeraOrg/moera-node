package org.moera.node.liberin.model;

import org.moera.lib.node.types.SheriffComplaintStatus;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.liberin.Liberin;

public class SheriffComplaintGroupUpdatedLiberin extends Liberin {

    private SheriffComplaintGroup group;
    private SheriffComplaintStatus prevStatus;

    public SheriffComplaintGroupUpdatedLiberin(SheriffComplaintGroup group, SheriffComplaintStatus prevStatus) {
        this.group = group;
        this.prevStatus = prevStatus;
    }

    public SheriffComplaintGroup getGroup() {
        return group;
    }

    public void setGroup(SheriffComplaintGroup group) {
        this.group = group;
    }

    public SheriffComplaintStatus getPrevStatus() {
        return prevStatus;
    }

    public void setPrevStatus(SheriffComplaintStatus prevStatus) {
        this.prevStatus = prevStatus;
    }

}
