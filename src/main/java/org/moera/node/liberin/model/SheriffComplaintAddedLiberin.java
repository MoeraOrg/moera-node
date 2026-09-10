package org.moera.node.liberin.model;

import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.liberin.Liberin;

public class SheriffComplaintAddedLiberin extends Liberin {

    private SheriffComplaint complaint;
    private SheriffComplaintGroup group;

    public SheriffComplaintAddedLiberin(SheriffComplaint complaint, SheriffComplaintGroup group) {
        this.complaint = complaint;
        this.group = group;
    }

    public SheriffComplaint getComplaint() {
        return complaint;
    }

    public void setComplaint(SheriffComplaint complaint) {
        this.complaint = complaint;
    }

    public SheriffComplaintGroup getGroup() {
        return group;
    }

    public void setGroup(SheriffComplaintGroup group) {
        this.group = group;
    }

}
