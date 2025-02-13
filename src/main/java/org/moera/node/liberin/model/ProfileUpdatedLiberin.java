package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarInfoUtil;
import org.moera.node.model.ProfileInfoUtil;
import org.moera.node.option.Options;

public class ProfileUpdatedLiberin extends Liberin {

    private String nodeName;
    private Options options;
    private Avatar avatar;
    private String prevEmail;

    public ProfileUpdatedLiberin(String nodeName, Options options, Avatar avatar, String prevEmail) {
        this.nodeName = nodeName;
        this.options = options;
        this.avatar = avatar;
        this.prevEmail = prevEmail;
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

    public String getPrevEmail() {
        return prevEmail;
    }

    public void setPrevEmail(String prevEmail) {
        this.prevEmail = prevEmail;
    }

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("profile", ProfileInfoUtil.build(options));
        model.put("avatar", AvatarInfoUtil.build(avatar));
        model.put("prevEmail", prevEmail);
    }

}
