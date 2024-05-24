package org.moera.node.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.moera.naming.rpc.OperationStatus;
import org.moera.node.global.RequestContext;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WhoAmI {

    private String nodeName;
    private boolean nodeNameChanging;
    private String fullName;
    private String gender;
    private String title;
    private AvatarImage avatar;
    private Boolean frozen;

    public WhoAmI() {
    }

    public WhoAmI(RequestContext requestContext) {
        nodeName = requestContext.nodeName();
        OperationStatus status = OperationStatus.forValue(
                requestContext.getOptions().getString("naming.operation.status"));
        nodeNameChanging = status == OperationStatus.WAITING
                || status == OperationStatus.ADDED
                || status == OperationStatus.STARTED;
        fullName = requestContext.fullName();
        gender = requestContext.getOptions().getString("profile.gender");
        title = requestContext.getOptions().getString("profile.title");
        if (requestContext.getAvatar() != null) {
            avatar = new AvatarImage(requestContext.getAvatar());
        }
        if (requestContext.getOptions().getBool("frozen")) {
            frozen = true;
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

    public Boolean getFrozen() {
        return frozen;
    }

    public void setFrozen(Boolean frozen) {
        this.frozen = frozen;
    }

}
