package org.moera.node.model.event;

import java.util.List;

import org.moera.lib.node.types.Scope;
import org.moera.lib.util.LogUtil;
import org.moera.node.data.Avatar;
import org.moera.node.model.AvatarImage;
import org.moera.node.option.Options;
import org.springframework.data.util.Pair;

public class NodeNameChangedEvent extends Event {

    private String name;
    private String fullName;
    private String gender;
    private String title;
    private AvatarImage avatar;

    public NodeNameChangedEvent() {
        super(EventType.NODE_NAME_CHANGED, Scope.IDENTIFY);
    }

    public NodeNameChangedEvent(String name, Options options, Avatar avatar) {
        this();
        this.name = name;
        fullName = options.getString("profile.full-name");
        gender = options.getString("profile.gender");
        title = options.getString("profile.title");
        if (avatar != null) {
            this.avatar = new AvatarImage(avatar);
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    @Override
    public void logParameters(List<Pair<String, String>> parameters) {
        super.logParameters(parameters);
        parameters.add(Pair.of("name", LogUtil.format(name)));
        parameters.add(Pair.of("fullName", LogUtil.format(fullName)));
        parameters.add(Pair.of("gender", LogUtil.format(gender)));
        parameters.add(Pair.of("title", LogUtil.format(title)));
        parameters.add(Pair.of("avatar", avatar != null ? avatar.toLogString() : "null"));
    }

}
