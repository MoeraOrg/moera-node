package org.moera.node.model;

import org.moera.lib.node.types.SettingDescriptor;
import org.moera.lib.node.types.SettingMetaInfo;

public class SettingMetaInfoUtil {

    public static SettingMetaInfo build(SettingDescriptor descriptor) {
        SettingMetaInfo info = new SettingMetaInfo();
        info.setName(descriptor.getName());
        info.setType(descriptor.getType());
        info.setDefaultValue(descriptor.getDefaultValue());
        info.setPrivileged(descriptor.getPrivileged());
        info.setTitle(descriptor.getTitle());
        info.setModifiers(descriptor.getModifiers());
        return info;
    }

}
