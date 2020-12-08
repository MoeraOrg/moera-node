package org.moera.node.option.type;

import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.Util;
import org.moera.node.webpush.WebPush;

@OptionType("PublicKeyWebPush")
public class PublicKeyWebPushOptionType extends PublicKeyOptionType {

    @Override
    public Object deserializeValue(String value) {
        return CryptoUtil.toPublicKey(Util.base64decode(value), WebPush.EC_CURVE);
    }

}
