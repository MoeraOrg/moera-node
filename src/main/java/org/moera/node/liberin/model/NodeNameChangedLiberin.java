package org.moera.node.liberin.model;

import org.moera.node.data.Avatar;
import org.moera.node.liberin.Liberin;
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

}
