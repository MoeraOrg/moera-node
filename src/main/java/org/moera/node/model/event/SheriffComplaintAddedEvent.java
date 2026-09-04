package org.moera.node.model.event;

import java.util.List;
import java.util.UUID;

import org.moera.lib.node.types.Scope;
import org.moera.lib.node.types.SheriffComplaintInfo;
import org.moera.lib.node.types.SheriffOrderReason;
import org.moera.lib.node.types.principal.Principal;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.model.SheriffComplaintInfoUtil;

public class SheriffComplaintAddedEvent extends Event {

    private SheriffComplaintInfo complaint;
    private String groupId;

    public SheriffComplaintAddedEvent() {
        super(EventType.SHERIFF_COMPLAINT_ADDED, Scope.SHERIFF, Principal.ADMIN);
    }

    public SheriffComplaintAddedEvent(SheriffComplaint complaint, UUID groupId) {
        super(EventType.SHERIFF_COMPLAINT_ADDED, Scope.SHERIFF, Principal.ADMIN);
        this.complaint = SheriffComplaintInfoUtil.build(complaint, false);
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
    public void logParameters(List<LogParameter> parameters) {
        super.logParameters(parameters);
        parameters.add(new LogParameter("id", LogUtil.format(complaint.getId())));
        parameters.add(new LogParameter("ownerName", LogUtil.format(complaint.getOwnerName())));
        parameters.add(new LogParameter(
            "reasonCode", LogUtil.format(SheriffOrderReason.toValue(complaint.getReasonCode()))
        ));
        parameters.add(new LogParameter("groupId", LogUtil.format(groupId)));
    }

}
