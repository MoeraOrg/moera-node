package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffComplainGroupInfo;

public class SheriffComplainGroupAddedLiberin extends Liberin {

    private SheriffComplainGroup group;

    public SheriffComplainGroupAddedLiberin(SheriffComplainGroup group) {
        this.group = group;
    }

    public SheriffComplainGroup getGroup() {
        return group;
    }

    public void setGroup(SheriffComplainGroup group) {
        this.group = group;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("group", new SheriffComplainGroupInfo(group));
    }

}
