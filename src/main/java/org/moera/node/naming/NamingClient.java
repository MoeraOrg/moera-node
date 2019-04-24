package org.moera.node.naming;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.moera.commons.util.CryptoException;
import org.moera.commons.util.CryptoUtil;
import org.moera.commons.util.Util;
import org.moera.naming.rpc.NamingService;
import org.moera.naming.rpc.OperationStatus;
import org.moera.naming.rpc.OperationStatusInfo;
import org.moera.naming.rpc.PutSignatureDataBuilder;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.naming.rpc.Rules;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
import org.moera.node.option.OptionsLoadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class NamingClient {

    private static Logger log = LoggerFactory.getLogger(NamingClient.class);

    private NamingService namingService;

    @Inject
    private Options options;

    @Inject
    private TaskScheduler taskScheduler;

    @PostConstruct
    protected void init() throws MalformedURLException {
        JsonRpcHttpClient client = new JsonRpcHttpClient(new URL(options.getString("naming.location")));
        namingService = ProxyUtil.createClientProxy(getClass().getClassLoader(), NamingService.class, client);
    }

    @EventListener(OptionsLoadedEvent.class)
    public void optionsLoaded() {
        monitorOperation();
    }

    private void monitorOperation() {
        UUID operationId = options.getUuid("naming.operation.id");
        if (operationId == null) {
            log.info("No pending naming operation");
            return;
        }
        log.info("Started monitoring for naming operation {}", operationId);
        options.set("naming.operation.status", OperationStatus.WAITING.getValue());
        final AtomicInteger retries = new AtomicInteger(0);
        taskScheduler.schedule(() -> {
            UUID id = options.getUuid("naming.operation.id");
            if (id == null) {
                return;
            }
            log.debug("Monitoring naming operation {}", id);

            OperationStatusInfo info;
            try {
                info = namingService.getStatus(id);
            } catch (Exception e) {
                options.set("naming.operation.status.updated", Util.now());
                int n = retries.incrementAndGet();
                int maxRetries = options.getInt("naming.unavailable.max-retries");
                if (n > maxRetries) {
                    unknownOperationStatus();
                } else {
                    log.info("Naming service unavailable, retry {} of {}...", n, maxRetries);
                }
                return;
            }
            retries.set(0);
            if (info.getStatus() == null) {
                unknownOperationStatus();
                return;
            }
            log.info("Naming operation {}, status is {}", id, info.getStatus().name());
            options.set("naming.operation.status", info.getStatus().getValue());
            options.set("naming.operation.added", info.getAdded());
            options.set("naming.operation.status.updated", Util.now());
            switch (info.getStatus()) {
                case ADDED:
                case STARTED:
                    break;
                case SUCCEEDED:
                    options.set("naming.operation.completed", info.getCompleted());
                    commitOperation();
                    options.set("profile.registered-name.generation", info.getGeneration());
                    options.reset("naming.operation.id");
                    break;
                case FAILED:
                    options.set("naming.operation.completed", info.getCompleted());
                    options.set("naming.operation.error-code", "naming." + info.getErrorCode());
                    options.set("naming.operation.error-message", info.getErrorMessage());
                    options.reset("naming.operation.id");
                    break;
            }
            if (options.getUuid("naming.operation.id") == null) {
                log.info("Stopped monitoring naming operation {}", id);
            }
        }, context -> {
            if (options.getUuid("naming.operation.id") == null) {
                return null;
            }
            Date last = context.lastCompletionTime();
            return last == null ? new Date() : Date.from(last.toInstant().plusSeconds(60));
        });
    }

    private void unknownOperationStatus() {
        log.info("Status of naming operation {} is set to 'unknown'", options.getString("naming.operation.id"));

        options.set("naming.operation.status", OperationStatus.UNKNOWN.getValue());
        options.set("naming.operation.status.updated", Util.now());
        options.set("naming.operation.error-code", "naming.unknown");
        options.set("naming.operation.error-message", "operation status is unknown");
    }

    private void operationSent(UUID operationId) {
        log.info("Created naming operation {}", operationId);

        options.set("naming.operation.id", operationId);
        options.set("naming.operation.status", OperationStatus.WAITING.getValue());
        options.set("naming.operation.status.updated", Util.now());
        options.reset("naming.operation.error-code");
        options.reset("naming.operation.error-message");
        options.reset("naming.operation.completed");
        options.reset("naming.operation.registered-name");
        options.reset("naming.operation.registered-name.generation");
        options.reset("naming.operation.signing-key");
    }

    private void commitOperation() {
        String name = options.getString("naming.operation.registered-name");
        if (name != null) {
            options.set("profile.registered-name", name);
        }
        Integer generation = options.getInt("naming.operation.registered-name.generation");
        if (generation != null) {
            options.set("profile.registered-name.generation", generation);
        }
        PrivateKey signingKey = options.getPrivateKey("naming.operation.signing-key");
        if (signingKey != null) {
            options.set("profile.signing-key", signingKey);
        }
    }

    public void register(String name, String nodeUri, ECPublicKey updatingKey,
                         ECPrivateKey privateSigningKey, ECPublicKey signingKey) {

        byte[] updatingKeyR = CryptoUtil.toRawPublicKey(updatingKey);
        byte[] signingKeyR = CryptoUtil.toRawPublicKey(signingKey);
        long validFrom = Instant.now()
                                .plus(options.getDuration("profile.signing-key.valid-from.layover"))
                                .getEpochSecond();
        log.info("Registering name '{}': node uri = {}, updating key = {}, signing key = {}, valid from = {}",
                name, nodeUri, Util.dump(updatingKeyR), Util.dump(signingKeyR), Util.formatTimestamp(validFrom));
        UUID operationId;
        try {
            RegisteredNameInfo info = namingService.getCurrentForLatest(name);
            byte[] previousDigest = info != null ? info.getDigest() : null;
            operationId = namingService.put(
                    name,
                    false,
                    updatingKeyR,
                    nodeUri,
                    signingKeyR,
                    validFrom,
                    previousDigest,
                    null);
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        operationSent(operationId);
        options.set("naming.operation.registered-name", name);
        options.set("naming.operation.signing-key", privateSigningKey);
        monitorOperation();
    }

    public void update(String name, int generation, ECPrivateKey privateUpdatingKey,
                       ECPrivateKey privateSigningKey, ECPublicKey signingKey) {

        RegisteredNameInfo info;
        try {
            info = namingService.getCurrentForLatest(name);
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        if (info == null) {
            throw new OperationFailure("name-not-registered");
        }
        // TODO possible to validate the private key by the public key
        log.info("Updating name '{}', generation {}", name, generation);

        UUID operationId;
        try {
            byte[] previousDigest = info.getDigest();
            log.info("Previous digest is {}", previousDigest != null ? Util.dump(previousDigest) : "null");
            byte[] signingKeyR = signingKey != null ? CryptoUtil.toRawPublicKey(signingKey) : info.getSigningKey();
            long validFrom = signingKey != null
                    ? Instant.now()
                        .plus(options.getDuration("profile.signing-key.valid-from.layover"))
                        .getEpochSecond()
                    : info.getValidFrom();
            PutSignatureDataBuilder buf = new PutSignatureDataBuilder(
                    info.getName(),
                    info.getUpdatingKey(),
                    info.getNodeUri(),
                    info.getDeadline(),
                    signingKeyR,
                    validFrom,
                    info.getDigest());

            log.debug("Data to be signed: {}", Util.dump(buf.toBytes()));

            Signature signature = Signature.getInstance(Rules.SIGNATURE_ALGORITHM, "BC");
            signature.initSign(privateUpdatingKey, SecureRandom.getInstanceStrong());
            signature.update(buf.toBytes());

            try {
                operationId = namingService.put(
                        name,
                        false,
                        null,
                        null,
                        signingKey != null ? signingKeyR : null,
                        signingKey != null ? validFrom : null,
                        info.getDigest(),
                        signature.sign());
            } catch (Exception e) {
                throw new NamingNotAvailableException(e);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new CryptoException(e);
        }
        operationSent(operationId);
        options.set("naming.operation.registered-name", name);
        options.set("naming.operation.registered-name.generation", generation);
        if (privateSigningKey != null) {
            options.set("naming.operation.signing-key", privateSigningKey);
        }
        monitorOperation();
    }

}
