package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.SheriffComplaintStatus;
import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffComplaintGroupInfoUtil;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("group", SheriffComplaintGroupInfoUtil.build(group));
        model.put("prevStatus", prevStatus);
    }

}
