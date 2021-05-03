package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.naming.rpc.OperationStatus;
import org.moera.node.data.Avatar;
import org.moera.node.option.Options;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhoAmI {

    private String nodeName;
    private boolean nodeNameChanging;
    private String fullName;
    private String gender;
    private String title;
    private AvatarImage avatar;

    public WhoAmI() {
    }

    public WhoAmI(Options options, Avatar avatar) {
        nodeName = options.nodeName();
        OperationStatus status = OperationStatus.forValue(options.getString("naming.operation.status"));
        nodeNameChanging = status == OperationStatus.WAITING
                || status == OperationStatus.ADDED
                || status == OperationStatus.STARTED;
        fullName = options.getString("profile.full-name");
        gender = options.getString("profile.gender");
        title = options.getString("profile.title");
        if (avatar != null) {
            this.avatar = new AvatarImage(avatar);
        }
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public boolean isNodeNameChanging() {
        return nodeNameChanging;
    }

    public void setNodeNameChanging(boolean nodeNameChanging) {
        this.nodeNameChanging = nodeNameChanging;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public AvatarImage getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarImage avatar) {
        this.avatar = avatar;
    }

}
