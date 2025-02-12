package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.lib.node.types.AvatarImage;
import org.moera.node.liberin.Liberin;

public class AskedToSubscribeLiberin extends Liberin {

    private String remoteNodeName;
    private String remoteFullName;
    private String remoteGender;
    private AvatarImage remoteAvatar;
    private String message;

    public AskedToSubscribeLiberin(String remoteNodeName, String remoteFullName, String remoteGender,
                                   AvatarImage remoteAvatar, String message) {
        this.remoteNodeName = remoteNodeName;
        this.remoteFullName = remoteFullName;
        this.remoteGender = remoteGender;
        this.remoteAvatar = remoteAvatar;
        this.message = message;
    }

    public String getRemoteNodeName() {
        return remoteNodeName;
    }

    public void setRemoteNodeName(String remoteNodeName) {
        this.remoteNodeName = remoteNodeName;
    }

    public String getRemoteFullName() {
        return remoteFullName;
    }

    public void setRemoteFullName(String remoteFullName) {
        this.remoteFullName = remoteFullName;
    }

    public String getRemoteGender() {
        return remoteGender;
    }

    public void setRemoteGender(String remoteGender) {
        this.remoteGender = remoteGender;
    }

    public AvatarImage getRemoteAvatar() {
        return remoteAvatar;
    }

    public void setRemoteAvatar(AvatarImage remoteAvatar) {
        this.remoteAvatar = remoteAvatar;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("remoteNodeName", remoteNodeName);
        model.put("remoteFullName", remoteFullName);
        model.put("remoteGender", remoteGender);
        model.put("remoteAvatar", remoteAvatar);
        model.put("message", message);
    }

}
