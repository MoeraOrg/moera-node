package org.moera.node.liberin.model;

import java.util.Map;

import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
import org.moera.node.model.AvatarImage;
import org.moera.node.option.Options;

public class NodeNameChangedLiberin extends Liberin {

    private String nodeName;
    private Options options;
    private Avatar avatar;

    public NodeNameChangedLiberin(Options options, Avatar avatar) {
        this("", options, avatar);
    }

    public NodeNameChangedLiberin(String nodeName, Options options, Avatar avatar) {
        this.nodeName = nodeName;
        this.options = options;
        this.avatar = avatar;
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

    @Override
    protected void toModel(Map<String, Object> model) {
        super.toModel(model);
        model.put("nodeName", nodeName);
        model.put("fullName", options.getString("profile.full-name"));
        model.put("gender", options.getString("profile.gender"));
        model.put("title", options.getString("profile.title"));
        if (avatar != null) {
            model.put("avatar", new AvatarImage(avatar));
        }
    }

}
