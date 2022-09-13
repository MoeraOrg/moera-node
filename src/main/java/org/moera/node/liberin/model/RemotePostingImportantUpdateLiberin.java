package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;

public class RemotePostingImportantUpdateLiberin extends Liberin {

    private String nodeName;
    private String ownerName;
    private String ownerFullName;
    private AvatarImage ownerAvatar;
    private String id;
    private String heading;
    private String description;

    public RemotePostingImportantUpdateLiberin(String nodeName, String ownerName, String ownerFullName,
                                               AvatarImage ownerAvatar, String id, String heading, String description) {
        this.nodeName = nodeName;
        this.ownerName = ownerName;
        this.ownerFullName = ownerFullName;
        this.ownerAvatar = ownerAvatar;
        this.id = id;
        this.heading = heading;
        this.description = description;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("ownerName", ownerName);
        model.put("ownerFullName", ownerFullName);
        model.put("ownerAvatar", ownerAvatar);
        model.put("id", id);
        model.put("heading", heading);
        model.put("description", description);
    }

}
