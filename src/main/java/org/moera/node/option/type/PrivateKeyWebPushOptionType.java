package org.moera.node.option.type;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.Util;
import org.moera.node.webpush.WebPush;

@OptionType("PrivateKeyWebPush")
public class PrivateKeyWebPushOptionType extends PrivateKeyOptionType {

    @Override
    public Object deserializeValue(String value) {
        return CryptoUtil.toPrivateKey(Util.base64decode(value), WebPush.EC_CURVE);
    }

}
