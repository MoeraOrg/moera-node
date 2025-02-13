package org.moera.node.model;

import org.moera.lib.node.types.SettingInfo;

public class SettingInfoUtil {

    public static SettingInfo build(String name, String value) {
        SettingInfo settingInfo = new SettingInfo();
        settingInfo.setName(name);
        settingInfo.setValue(value);
        return settingInfo;
    }

}
