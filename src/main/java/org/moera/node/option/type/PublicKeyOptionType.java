package org.moera.node.option.type;

import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.node.util.Util;

@OptionType("PublicKey")
public class PublicKeyOptionType extends OptionTypeBase {

    @Override
    public String serializeValue(Object value) {
        return Util.base64encode(CryptoUtil.rawPublicKey((ECPublicKey) value));
    }

    @Override
    public Object deserializeValue(String value) {
        return CryptoUtil.rawToPublicKey(Util.base64decode(value));
    }

    @Override
    public String getString(Object value) {
        return serializeValue(value);
    }

    @Override
    public PublicKey getPublicKey(Object value) {
        return (PublicKey) value;
    }

    @Override
    protected Object accept(Object value) {
        if (value instanceof PublicKey) {
            return value;
        }
        return super.accept(value);
    }

}
