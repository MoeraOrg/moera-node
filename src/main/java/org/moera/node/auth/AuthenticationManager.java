package org.moera.node.auth;

import java.net.InetAddress;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import io.hypersistence.utils.hibernate.type.basic.Inet;
import org.hibernate.HibernateException;
import org.moera.lib.crypto.CryptoException;
import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.crypto.Fingerprint;
import org.moera.lib.crypto.FingerprintException;
import org.moera.lib.crypto.RestoredFingerprint;
import org.moera.lib.node.Fingerprints;
import org.moera.lib.util.LogUtil;
import org.moera.node.api.naming.NamingCache;
import org.moera.node.data.Token;
import org.moera.node.data.TokenRepository;
import org.moera.node.fingerprint.CarteProperties;
import org.moera.node.global.RequestContext;
import org.moera.node.global.UniversalContext;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class AuthenticationManager {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationManager.class);

    @Inject
    private RequestContext requestContext;

    @Inject
    private UniversalContext universalContext;

    @Inject
    private TokenRepository tokenRepository;

    @Inject
    @Lazy
    private NamingCache namingCache;

    @Inject
    private Transaction tx;

    @Inject
    private EntityManager entityManager;

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
        stampToken(token);
        lastUsedToken(token);
        return token;
    }

    private void stampToken(Token token) {
        Duration lifetime = universalContext.getOptions().getDuration("token.lifetime").getDuration();
        if (Instant.now().plus(lifetime).isAfter(token.getDeadline().toInstant().plus(1, ChronoUnit.HOURS))) {
            tx.executeWriteQuietly(
                () -> {
                    Token tk = entityManager.merge(token);
                    tk.setDeadline(Timestamp.from(Instant.now().plus(lifetime)));
                },
                e -> log.error("Could not stamp token {}: {}", LogUtil.format(token.getId()), e.getMessage())
            );
        }
    }

    private void lastUsedToken(Token token) {
        if (universalContext.isBackground()) {
            return;
        }
        if (token.getLastUsedAt() == null
                || token.getLastUsedAt().toInstant().plus(10, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            tx.executeWriteQuietly(
                () -> {
                    Token tk = entityManager.merge(token);
                    tk.setLastUsedAt(Util.now());
                    tk.setLastUsedBrowser(getUserAgentString());
                    if (requestContext.getRemoteAddr() != null) {
                        tk.setLastUsedIp(new Inet(requestContext.getRemoteAddr().getHostAddress()));
                    }
                },
                e -> log.error("Could not record the last usage of token {}: {}",
                        LogUtil.format(token.getId()), e.getMessage())
            );
        }
    }

    private String getUserAgentString() {
        StringBuilder buf = new StringBuilder();
        if (requestContext.getUserAgent() != null) {
            buf.append(requestContext.getUserAgent().getTitle());
        }
        if (requestContext.getUserAgentOs() != null) {
            buf.append('/');
            buf.append(requestContext.getUserAgentOs().getTitle());
        }
        return buf.toString();
    }

    public CarteAuthInfo getCarte(String carteS, InetAddress clientAddress) {
        if (ObjectUtils.isEmpty(carteS)) {
            return null;
        }
        byte[] carte = carteS.endsWith("=")
                ? Util.base64decode(carteS) // backward compatibility, remove later
                : Util.base64urldecode(carteS);
        if (carte.length == 0) {
            return null;
        }
        Fingerprint fingerprint;
        byte[] signature;
        try {
            RestoredFingerprint rc = CryptoUtil.restore(carte, version -> Fingerprints.getSchema("CARTE", version));
            fingerprint = rc.fingerprint();
            signature = new byte[rc.available()];
            System.arraycopy(carte, carte.length - signature.length, signature, 0, signature.length);
        } catch (CryptoException | FingerprintException e) {
            log.info("Carte: unknown fingerprint");
            throw new InvalidCarteException("carte.unknown-fingerprint", e);
        }
        CarteProperties cp = new CarteProperties(fingerprint);
        if (!"CARTE".equals(cp.getObjectType())) {
            log.info("Carte: not a carte fingerprint");
            throw new InvalidCarteException("carte.invalid");
        }
        if (cp.getAddress() != null && clientAddress != null && !cp.getAddress().equals(clientAddress)) {
            log.info("Carte: IP {} differs from client IP {}", cp.getAddress(), clientAddress);
            throw new InvalidCarteException("carte.invalid");
        }
        if (Instant.now().isBefore(cp.getBeginning().toInstant().minusSeconds(120))) {
            log.info("Carte: begins at {} - 2 min", LogUtil.format(cp.getBeginning()));
            throw new InvalidCarteException("carte.not-begun");
        }
        if (Instant.now().isAfter(cp.getDeadline().toInstant().plusSeconds(120))) {
            log.info("Carte: deadline at {} + 2 min", LogUtil.format(cp.getDeadline()));
            throw new InvalidCarteException("carte.expired");
        }
        if (cp.getNodeName() != null && !cp.getNodeName().equals(requestContext.getOptions().nodeName())) {
            log.info("Carte: belongs to a wrong node ({})", LogUtil.format(cp.getNodeName()));
            throw new InvalidCarteException("carte.wrong-node");
        }
        byte[] signingKey = namingCache.get(cp.getOwnerName()).getSigningKey();
        if (signingKey == null) {
            log.info("Carte: signing key for node {} is unknown", LogUtil.format(cp.getOwnerName()));
            throw new InvalidCarteException("carte.unknown-signing-key");
        }
        byte[] fingerprintBytes = CryptoUtil.fingerprint(
            fingerprint, Fingerprints.getSchema("CARTE", fingerprint.getVersion())
        );
        if (!CryptoUtil.verify(fingerprintBytes, signature, signingKey)) {
            log.info("Carte: signature verification failed");
            throw new InvalidCarteException("carte.invalid-signature");
        }
        return new CarteAuthInfo(cp);
    }

}
