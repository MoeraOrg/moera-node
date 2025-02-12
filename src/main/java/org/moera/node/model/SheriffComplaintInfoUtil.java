package org.moera.node.model;

import org.moera.lib.node.types.SheriffComplaintInfo;
import org.moera.node.data.SheriffComplaint;
import org.moera.node.util.Util;

public class SheriffComplaintInfoUtil {

    public static SheriffComplaintInfo build(SheriffComplaint sheriffComplaint, boolean withGroup) {
        SheriffComplaintInfo info = new SheriffComplaintInfo();
        info.setId(sheriffComplaint.getId().toString());
        info.setOwnerName(sheriffComplaint.getOwnerName());
        info.setOwnerFullName(sheriffComplaint.getOwnerFullName());
        info.setOwnerGender(sheriffComplaint.getOwnerGender());
        if (withGroup && sheriffComplaint.getGroup() != null) {
            info.setGroup(SheriffComplaintGroupInfoUtil.build(sheriffComplaint.getGroup()));
        }
        info.setReasonCode(sheriffComplaint.getReasonCode());
        info.setReasonDetails(sheriffComplaint.getReasonDetails());
        info.setAnonymousRequested(sheriffComplaint.isAnonymousRequested());
        info.setCreatedAt(Util.toEpochSecond(sheriffComplaint.getCreatedAt()));
        return info;
    }

}
