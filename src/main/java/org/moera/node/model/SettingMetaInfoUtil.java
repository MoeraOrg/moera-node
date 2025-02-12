package org.moera.node.model;

import org.moera.lib.node.types.SettingMetaInfo;
import org.moera.node.option.OptionDescriptor;

public class SettingMetaInfoUtil {

    public static SettingMetaInfo build(OptionDescriptor descriptor) {
        SettingMetaInfo info = new SettingMetaInfo();
        info.setName(descriptor.getName());
        info.setType(descriptor.getType());
        info.setDefaultValue(descriptor.getDefaultValue());
        info.setPrivileged(descriptor.isPrivileged());
        info.setTitle(descriptor.getTitle());
        info.setModifiers(SettingTypeModifiersUtil.build(descriptor.getModifiers()));
        return info;
    }

}
