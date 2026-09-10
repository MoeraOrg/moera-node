package org.moera.node.liberin.model;

import java.util.List;

import org.moera.lib.node.types.AvatarImage;
import org.moera.lib.node.types.SheriffMark;
import org.moera.node.liberin.Liberin;

public class MentionInRemotePostingAddedLiberin extends Liberin {

    private String nodeName;
    private String ownerName;
    private String ownerFullName;
    private String ownerSourceUri;
    private String ownerGender;
    private AvatarImage ownerAvatar;
    private String id;
    private String heading;
    private List<String> sheriffs;
    private List<SheriffMark> sheriffMarks;

    public MentionInRemotePostingAddedLiberin(
        String nodeName,
        String ownerName,
        String ownerFullName, String ownerSourceUri,
        String ownerGender,
        AvatarImage ownerAvatar,
        String id,
        String heading,
        List<String> sheriffs,
        List<SheriffMark> sheriffMarks
    ) {
        this.nodeName = nodeName;
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerSourceUri = ownerSourceUri;
        this.ownerGender = ownerGender;
        this.ownerAvatar = ownerAvatar;
        this.id = id;
        this.heading = heading;
        this.sheriffs = sheriffs;
        this.sheriffMarks = sheriffMarks;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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

    public String getOwnerSourceUri() {
        return ownerSourceUri;
    }

    public void setOwnerSourceUri(String ownerSourceUri) {
        this.ownerSourceUri = ownerSourceUri;
    }

    public String getOwnerGender() {
        return ownerGender;
    }

    public void setOwnerGender(String ownerGender) {
        this.ownerGender = ownerGender;
    }

    public AvatarImage getOwnerAvatar() {
        return ownerAvatar;
    }

    public void setOwnerAvatar(AvatarImage ownerAvatar) {
        this.ownerAvatar = ownerAvatar;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
