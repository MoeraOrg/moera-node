package org.moera.node.liberin.model;

import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
import org.moera.node.option.Options;

public class ProfileUpdatedLiberin extends Liberin {

    private String nodeName;
    private Options options;
    private Avatar avatar;
    private String oldEmail;

    public ProfileUpdatedLiberin(String nodeName, Options options, Avatar avatar, String oldEmail) {
        this.nodeName = nodeName;
        this.options = options;
        this.avatar = avatar;
        this.oldEmail = oldEmail;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public Options getOptions() {
        return options;
    }

    public void setOptions(Options options) {
        this.options = options;
    }

    public Avatar getAvatar() {
        return avatar;
    }

    public void setAvatar(Avatar avatar) {
        this.avatar = avatar;
    }

    public String getOldEmail() {
        return oldEmail;
    }

    public void setOldEmail(String oldEmail) {
        this.oldEmail = oldEmail;
    }

}
