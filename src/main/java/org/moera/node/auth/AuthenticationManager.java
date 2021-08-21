package org.moera.node.auth;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.commons.crypto.CryptoException;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.crypto.FingerprintException;
import org.moera.commons.crypto.RestoredObject;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.fingerprint.CarteFingerprint;
import org.moera.node.fingerprint.CarteProperties;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.global.RequestContext;
import org.moera.node.naming.NamingCache;
import org.moera.node.util.Util;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticationManager {

    @Inject
    private RequestContext requestContext;

    @Inject
    private TokenRepository tokenRepository;

    @Inject
    private FingerprintManager fingerprintManager;

    @Inject
    private NamingCache namingCache;

    public boolean isAdminToken(String tokenS, UUID nodeId) throws InvalidTokenException {
        if (!StringUtils.isEmpty(tokenS)) {
            Token token = tokenRepository.findById(tokenS).orElse(null);
            if (token == null
                    || !token.getNodeId().equals(nodeId)
                    || token.getDeadline().before(Util.now())) {
                throw new InvalidTokenException();
            }
            return token.isAdmin();
        }
        return false;
    }

    public String getClientName(String carteS, InetAddress clientAddress) {
        if (StringUtils.isEmpty(carteS)) {
            return null;
        }
        byte[] carte = Util.base64decode(carteS);
        if (carte.length == 0) {
            return null;
        }
        CarteProperties fp;
        byte[] signature;
        try {
            RestoredObject<Fingerprint> rc = CryptoUtil.restore(carte, this::carteFingerprintCreator);
            fp = (CarteProperties) rc.getObject();
            signature = new byte[rc.getAvailable()];
            System.arraycopy(carte, carte.length - signature.length, signature, 0, signature.length);
        } catch (CryptoException | FingerprintException e) {
            throw new InvalidCarteException("carte.unknown-fingerprint", e);
        }
        if (!FingerprintObjectType.CARTE.name().equals(fp.getObjectType())) {
            throw new InvalidCarteException("carte.invalid");
        }
        if (!clientAddress.equals(fp.getAddress())) {
            throw new InvalidCarteException("carte.invalid");
        }
        if (Instant.now().isBefore(Instant.ofEpochSecond(fp.getBeginning()).minusSeconds(120))) {
            throw new InvalidCarteException("carte.not-begun");
        }
        if (Instant.now().isAfter(Instant.ofEpochSecond(fp.getDeadline()).plusSeconds(120))) {
            throw new InvalidCarteException("carte.expired");
        }
        if (fp instanceof CarteFingerprint) {
            String nodeName = ((CarteFingerprint) fp).getNodeName();
            if (nodeName != null && !nodeName.equals(requestContext.getOptions().nodeName())) {
                throw new InvalidCarteException("carte.wrong-node");
            }
        }
        byte[] signingKey = namingCache.get(fp.getOwnerName()).getSigningKey();
        if (signingKey == null) {
            throw new InvalidCarteException("carte.unknown-signing-key");
        }
        if (!CryptoUtil.verify(fp, signature, signingKey)) {
            throw new InvalidCarteException("carte.invalid-signature");
        }
        return fp.getOwnerName();
    }

    private Fingerprint carteFingerprintCreator(short version) {
        Class<? extends Fingerprint> fingerprintClass = fingerprintManager.get(FingerprintObjectType.CARTE, version);
        if (fingerprintClass == null) {
            throw new InvalidCarteException("carte.unknown-fingerprint");
        }
        try {
            return fingerprintClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new InvalidCarteException("carte.unknown-fingerprint", e);
        }
    }

}
