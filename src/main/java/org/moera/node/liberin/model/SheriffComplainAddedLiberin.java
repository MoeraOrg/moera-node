package org.moera.node.liberin.model;

import java.util.Map;
import java.util.UUID;

import org.moera.node.data.SheriffComplain;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.SheriffComplainInfo;

public class SheriffComplainAddedLiberin extends Liberin {

    private SheriffComplain complain;
    private UUID groupId;

    public SheriffComplainAddedLiberin(SheriffComplain complain, UUID groupId) {
        this.complain = complain;
        this.groupId = groupId;
    }

    public SheriffComplain getComplain() {
        return complain;
    }

    public void setComplain(SheriffComplain complain) {
        this.complain = complain;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public void setGroupId(UUID groupId) {
        this.groupId = groupId;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("complain", new SheriffComplainInfo(complain, false));
        model.put("groupId", groupId);
    }

}
