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
import org.moera.commons.util.Util;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.fingerprint.CarteFingerprint;
import org.moera.node.fingerprint.FingerprintManager;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.naming.NamingCache;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthenticationManager {

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
        short version = carte[0]; // TODO limited to 127 versions
        CarteFingerprint fp;
        byte[] signature;
        try {
            RestoredObject<CarteFingerprint> rc = CryptoUtil.restore(carte, this::carteFingerprintCreator);
            fp = rc.getObject();
            signature = new byte[rc.getAvailable()];
            System.arraycopy(carte, carte.length - signature.length, signature, 0, signature.length);
        } catch (CryptoException | FingerprintException e) {
            throw new InvalidCarteException("carte.unknown-fingerprint", e);
        }
        if (!FingerprintObjectType.CARTE.name().equals(fp.objectType)) {
            throw new InvalidCarteException("carte.invalid");
        }
        if (!clientAddress.equals(fp.address)) {
            throw new InvalidCarteException("carte.invalid");
        }
        if (Instant.now().isBefore(Instant.ofEpochSecond(fp.beginning).minusSeconds(60))) {
            throw new InvalidCarteException("carte.not-begun");
        }
        if (Instant.now().isAfter(Instant.ofEpochSecond(fp.deadline).plusSeconds(60))) {
            throw new InvalidCarteException("carte.expired");
        }
        byte[] signingKey = namingCache.get(fp.ownerName).getSigningKey();
        if (signingKey == null) {
            throw new InvalidCarteException("carte.unknown-signing-key");
        }
        if (!CryptoUtil.verify(fp, signature, signingKey)) {
            throw new InvalidCarteException("carte.invalid-signature");
        }
        return fp.ownerName;
    }

    private CarteFingerprint carteFingerprintCreator(short version) {
        Class<? extends Fingerprint> fingerprintClass = fingerprintManager.get(FingerprintObjectType.CARTE, version);
        if (fingerprintClass == null) {
            throw new InvalidCarteException("carte.unknown-fingerprint");
        }
        try {
            return (CarteFingerprint) fingerprintClass.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new InvalidCarteException("carte.unknown-fingerprint", e);
        }
    }

}
