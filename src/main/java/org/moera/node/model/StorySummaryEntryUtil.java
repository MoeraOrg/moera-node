package org.moera.node.model;

import java.util.List;

import org.moera.lib.node.types.SheriffMark;
import org.moera.lib.node.types.StorySummaryEntry;

public class StorySummaryEntryUtil {

    public static StorySummaryEntry build(String ownerName, String ownerFullName, String ownerGender, String heading) {
        return build(ownerName, ownerFullName, ownerGender, heading, null, null);
    }

    public static StorySummaryEntry build(
        String ownerName,
        String ownerFullName,
        String ownerGender,
        String heading,
        List<String> sheriffs,
        List<SheriffMark> sheriffMarks
    ) {
        StorySummaryEntry entry = new StorySummaryEntry();
        entry.setOwnerName(ownerName);
        entry.setOwnerFullName(ownerFullName);
        entry.setOwnerGender(ownerGender);
        entry.setHeading(heading);
        entry.setSheriffs(sheriffs);
        entry.setSheriffMarks(sheriffMarks);
        return entry;
    }

}
