package org.moera.node.webpush;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;

import io.github.novacrypto.bip39.Words;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECPoint;
import org.moera.commons.crypto.CryptoException;
import org.moera.node.option.Options;
import org.springframework.stereotype.Service;

@Service
public class WebPushService {

    public PublicKey generateKeys(Options options) {
        try {
            ECParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(WebPush.EC_CURVE);

            SecureRandom random = new SecureRandom();
            byte[] seed = new byte[Words.TWENTY_FOUR.byteLength()];
            random.nextBytes(seed);

            BigInteger d = new BigInteger(seed);
            ECPoint q = ecSpec.getG().multiply(d);

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC", "BC");
            keyPairGenerator.initialize(ecSpec, random);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            options.set("web-push.private-key", keyPair.getPrivate());
            options.set("web-push.public-key", keyPair.getPublic());

            return keyPair.getPublic();
        } catch (GeneralSecurityException e) {
            throw new CryptoException(e);
        }
    }

}
