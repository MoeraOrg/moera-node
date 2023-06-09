package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.SheriffComplain;
import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffComplainGroupInfo;
import org.moera.node.model.SheriffComplainInfo;

public class SheriffComplainAddedLiberin extends Liberin {

    private SheriffComplain complain;
    private SheriffComplainGroup group;

    public SheriffComplainAddedLiberin(SheriffComplain complain, SheriffComplainGroup group) {
        this.complain = complain;
        this.group = group;
    }

    public SheriffComplain getComplain() {
        return complain;
    }

    public void setComplain(SheriffComplain complain) {
        this.complain = complain;
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
        model.put("complain", new SheriffComplainInfo(complain, false));
        model.put("group", new SheriffComplainGroupInfo(group));
    }

}
