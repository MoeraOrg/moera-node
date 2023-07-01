package org.moera.node.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.node.data.SheriffMark;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorySummaryEntry {

    private String ownerName;
    private String ownerFullName;
    private String ownerGender;
    private String heading;
    private List<String> sheriffs;
    private List<SheriffMark> sheriffMarks;

    public StorySummaryEntry() {
    }

    public StorySummaryEntry(String ownerName, String ownerFullName, String ownerGender, String heading) {
        this(ownerName, ownerFullName, ownerGender, heading, null, null);
    }

    public StorySummaryEntry(String ownerName, String ownerFullName, String ownerGender, String heading,
                             List<String> sheriffs, List<SheriffMark> sheriffMarks) {
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerGender = ownerGender;
        this.heading = heading;
        this.sheriffs = sheriffs;
        this.sheriffMarks = sheriffMarks;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public void setOwnerFullName(String ownerFullName) {
        this.ownerFullName = ownerFullName;
    }

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
    }

    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    public List<String> getSheriffs() {
        return sheriffs;
    }

    public void setSheriffs(List<String> sheriffs) {
        this.sheriffs = sheriffs;
    }

    public List<SheriffMark> getSheriffMarks() {
        return sheriffMarks;
    }

    public void setSheriffMarks(List<SheriffMark> sheriffMarks) {
        this.sheriffMarks = sheriffMarks;
    }

}
