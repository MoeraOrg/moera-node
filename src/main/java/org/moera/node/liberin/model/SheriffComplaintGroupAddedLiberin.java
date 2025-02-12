package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.SheriffComplaintGroup;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffComplaintGroupInfoUtil;

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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("group", SheriffComplaintGroupInfoUtil.build(group));
    }

}
