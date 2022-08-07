package org.moera.node.auth;

import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;
import javax.inject.Inject;

import org.hibernate.HibernateException;
import org.moera.commons.crypto.CryptoException;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.crypto.Fingerprint;
import org.moera.commons.crypto.FingerprintException;
import org.moera.commons.crypto.RestoredObject;
import org.moera.commons.util.LogUtil;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.fingerprint.CarteFingerprint;
import org.moera.node.fingerprint.CarteProperties;
import org.moera.node.fingerprint.FingerprintObjectType;
import org.moera.node.fingerprint.Fingerprints;
import org.moera.node.global.RequestContext;
import org.moera.node.naming.NamingCache;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationManager.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private TokenRepository tokenRepository;

    @Inject
    private NamingCache namingCache;

    public Token getToken(String tokenS, UUID nodeId) throws InvalidTokenException {
        if (ObjectUtils.isEmpty(tokenS)) {
            return null;
        }
        Token token = tokenRepository.findByToken(nodeId, tokenS, Util.now()).orElse(null);
        if (token == null) {
            log.info("Admin token provided is not found");
            throw new InvalidTokenException();
        }
        try {
            if (token.getIp() != null && !token.getIp().toInetAddress().equals(requestContext.getRemoteAddr())) {
                log.info("Admin token is bound to a different IP address");
                throw new InvalidTokenException();
            }
        } catch (HibernateException e) {
            log.error("Cannot resolve token address {}", token.getIp(), e);
            throw new InvalidTokenException();
        }
        return token;
    }

    public CarteAuthInfo getCarte(String carteS, InetAddress clientAddress) {
        if (ObjectUtils.isEmpty(carteS)) {
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
            log.info("Carte: unknown fingerprint");
            throw new InvalidCarteException("carte.unknown-fingerprint", e);
        }
        if (!FingerprintObjectType.CARTE.name().equals(fp.getObjectType())) {
            log.info("Carte: not a carte fingerprint");
            throw new InvalidCarteException("carte.invalid");
        }
        if (!clientAddress.equals(fp.getAddress())) {
            log.info("Carte: IP {} differs from client IP {}", fp.getAddress(), clientAddress);
            throw new InvalidCarteException("carte.invalid");
        }
        if (Instant.now().isBefore(Instant.ofEpochSecond(fp.getBeginning()).minusSeconds(120))) {
            log.info("Carte: begins at {} - 2 min", LogUtil.format(Util.toTimestamp(fp.getBeginning())));
            throw new InvalidCarteException("carte.not-begun");
        }
        if (Instant.now().isAfter(Instant.ofEpochSecond(fp.getDeadline()).plusSeconds(120))) {
            log.info("Carte: deadline at {} + 2 min", LogUtil.format(Util.toTimestamp(fp.getDeadline())));
            throw new InvalidCarteException("carte.expired");
        }
        if (fp instanceof CarteFingerprint) {
            String nodeName = ((CarteFingerprint) fp).getNodeName();
            if (nodeName != null && !nodeName.equals(requestContext.getOptions().nodeName())) {
                log.info("Carte: belongs to a wrong node ({})", LogUtil.format(nodeName));
                throw new InvalidCarteException("carte.wrong-node");
            }
        }
        byte[] signingKey = namingCache.get(fp.getOwnerName()).getSigningKey();
        if (signingKey == null) {
            log.info("Carte: signing key for node {} is unknown", LogUtil.format(fp.getOwnerName()));
            throw new InvalidCarteException("carte.unknown-signing-key");
        }
        if (!CryptoUtil.verify(fp, signature, signingKey)) {
            log.info("Carte: signature verification failed");
            throw new InvalidCarteException("carte.invalid-signature");
        }
        return new CarteAuthInfo(fp);
    }

    private Fingerprint carteFingerprintCreator(short version) {
        Class<? extends Fingerprint> fingerprintClass = Fingerprints.get(FingerprintObjectType.CARTE, version);
        if (fingerprintClass == null) {
            log.info("Carte: unknown fingerprint");
            throw new InvalidCarteException("carte.unknown-fingerprint");
        }
        try {
            return fingerprintClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            log.info("Carte: unknown fingerprint");
            throw new InvalidCarteException("carte.unknown-fingerprint", e);
        }
    }

}
