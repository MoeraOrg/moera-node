package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.SheriffComplainGroup;
import org.moera.node.data.SheriffComplainStatus;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffComplainGroupInfo;

public class SheriffComplainGroupUpdatedLiberin extends Liberin {

    private SheriffComplainGroup group;
    private SheriffComplainStatus prevStatus;

    public SheriffComplainGroupUpdatedLiberin(SheriffComplainGroup group, SheriffComplainStatus prevStatus) {
        this.group = group;
        this.prevStatus = prevStatus;
    }

    public SheriffComplainGroup getGroup() {
        return group;
    }

    public void setGroup(SheriffComplainGroup group) {
        this.group = group;
    }

    public SheriffComplainStatus getPrevStatus() {
        return prevStatus;
    }

    public void setPrevStatus(SheriffComplainStatus prevStatus) {
        this.prevStatus = prevStatus;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("group", new SheriffComplainGroupInfo(group));
        model.put("prevStatus", prevStatus);
    }

}
