package org.moera.node.model;

import java.util.Arrays;

import org.moera.lib.node.types.SettingTypeModifiers;
import org.moera.node.option.OptionTypeModifiers;
import org.moera.node.util.Util;

public class SettingTypeModifiersUtil {

    public static SettingTypeModifiers build(OptionTypeModifiers modifiers) {
        SettingTypeModifiers settingTypeModifiers = new SettingTypeModifiers();
        settingTypeModifiers.setFormat(modifiers.getFormat());
        settingTypeModifiers.setMin(modifiers.getMin());
        settingTypeModifiers.setMax(modifiers.getMax());
        settingTypeModifiers.setMultiline(Util.toBoolean(modifiers.getMultiline()));
        settingTypeModifiers.setNever(Util.toBoolean(modifiers.getNever()));
        settingTypeModifiers.setAlways(Util.toBoolean(modifiers.getAlways()));
        if (modifiers.getPrincipals() != null) {
            settingTypeModifiers.setPrincipals(Arrays.asList(modifiers.getPrincipals()));
        }
        return settingTypeModifiers;
    }
}
