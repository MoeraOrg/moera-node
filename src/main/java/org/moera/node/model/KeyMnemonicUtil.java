package org.moera.node.model;

import java.util.List;

import org.moera.lib.node.types.KeyMnemonic;

public class KeyMnemonicUtil {

    public static KeyMnemonic build(List<String> mnemonic) {
        KeyMnemonic keyMnemonic = new KeyMnemonic();
        keyMnemonic.setMnemonic(mnemonic);
        return keyMnemonic;
    }

}
