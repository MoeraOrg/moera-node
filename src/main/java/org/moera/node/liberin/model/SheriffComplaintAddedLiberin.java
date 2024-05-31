package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.SheriffComplaint;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffComplaintGroupInfo;
import org.moera.node.model.SheriffComplaintInfo;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("complaint", new SheriffComplaintInfo(complaint, false));
        model.put("group", new SheriffComplaintGroupInfo(group));
    }

}
