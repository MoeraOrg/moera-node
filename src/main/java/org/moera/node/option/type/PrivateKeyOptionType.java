package org.moera.node.option.type;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.node.util.Util;

@OptionType("PrivateKey")
public class PrivateKeyOptionType extends OptionTypeBase {

    @Override
    public String serializeValue(Object value) {
        return Util.base64encode(CryptoUtil.toRawPrivateKey((ECPrivateKey) value));
    }

    @Override
    public Object deserializeValue(String value) {
        return CryptoUtil.toPrivateKey(Util.base64decode(value));
    }

    @Override
    public String getString(Object value) {
        return serializeValue(value);
    }

    @Override
    public PrivateKey getPrivateKey(Object value) {
        return (PrivateKey) value;
    }

    @Override
    protected Object accept(Object value) {
        if (value instanceof PrivateKey) {
            return value;
        }
        return super.accept(value);
    }

}
