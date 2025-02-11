package org.moera.node.model.event;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SheriffOrderReason;
import org.moera.lib.util.LogUtil;
import org.moera.node.auth.principal.Principal;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.model.SheriffComplaintInfo;
import org.springframework.data.util.Pair;

public class SheriffComplaintAddedEvent extends Event {

    private SheriffComplaintInfo complaint;
    private String groupId;

    public SheriffComplaintAddedEvent() {
        super(EventType.SHERIFF_COMPLAINT_ADDED, Scope.SHERIFF, Principal.ADMIN);
    }

    public SheriffComplaintAddedEvent(SheriffComplaint complaint, UUID groupId) {
        super(EventType.SHERIFF_COMPLAINT_ADDED, Scope.SHERIFF, Principal.ADMIN);
        this.complaint = new SheriffComplaintInfo(complaint, false);
        this.groupId = groupId.toString();
    }

    public SheriffComplaintInfo getComplaint() {
        return complaint;
    }

    public void setComplaint(SheriffComplaintInfo complaint) {
        this.complaint = complaint;
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
        parameters.add(Pair.of("id", LogUtil.format(complaint.getId())));
        parameters.add(Pair.of("ownerName", LogUtil.format(complaint.getOwnerName())));
        parameters.add(Pair.of("reasonCode", LogUtil.format(SheriffOrderReason.toValue(complaint.getReasonCode()))));
        parameters.add(Pair.of("groupId", LogUtil.format(groupId)));
    }

}
