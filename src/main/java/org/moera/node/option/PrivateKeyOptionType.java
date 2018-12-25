package org.moera.node.option;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;

import org.moera.commons.util.CryptoUtil;
import org.moera.commons.util.Util;

@OptionType("PrivateKey")
public class PrivateKeyOptionType extends OptionTypeBase {

    @Override
    public String serializeValue(Object value) {
        return Util.base64encode(CryptoUtil.toRawPrivateKey((PrivateKey) value));
    }

    @Override
    public Object deserializeValue(String value) {
        try {
            return CryptoUtil.toPrivateKey(Util.base64decode(value));
        } catch (NoSuchAlgorithmException e) {
            throw new DeserializeOptionValueException("ECDSA algorithm is not available");
        } catch (InvalidKeySpecException e) {
            throw new DeserializeOptionValueException("Invalid value of type 'PrivateKey' for option");
        }
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
    public Object accept(Object value) {
        if (value instanceof PrivateKey) {
            return value;
        } else {
            return super.accept(value);
        }
    }

}
