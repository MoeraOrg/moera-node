package org.moera.node.model.event;

import java.util.List;
import java.util.UUID;

import org.moera.commons.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.SheriffComplain;
import org.moera.node.model.SheriffComplainInfo;
import org.moera.node.model.SheriffOrderReason;
import org.springframework.data.util.Pair;

public class SheriffComplainAddedEvent extends Event {

    private SheriffComplainInfo complain;
    private String groupId;

    public SheriffComplainAddedEvent() {
        super(EventType.SHERIFF_COMPLAIN_ADDED, Principal.ADMIN);
    }

    public SheriffComplainAddedEvent(SheriffComplain complain, UUID groupId) {
        super(EventType.SHERIFF_COMPLAIN_ADDED, Principal.ADMIN);
        this.complain = new SheriffComplainInfo(complain, false);
        this.groupId = groupId.toString();
    }

    public SheriffComplainInfo getComplain() {
        return complain;
    }

    public void setComplain(SheriffComplainInfo complain) {
        this.complain = complain;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("id", LogUtil.format(complain.getId())));
        parameters.add(Pair.of("ownerName", LogUtil.format(complain.getOwnerName())));
        parameters.add(Pair.of("reasonCode", LogUtil.format(SheriffOrderReason.toValue(complain.getReasonCode()))));
        parameters.add(Pair.of("groupId", LogUtil.format(groupId)));
    }

}
