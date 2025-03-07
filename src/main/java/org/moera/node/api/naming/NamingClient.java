package org.moera.node.api.naming;

import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.inject.Inject;

import org.moera.lib.crypto.CryptoUtil;
import org.moera.lib.naming.Fingerprints;
import org.moera.lib.naming.MoeraNaming;
import org.moera.lib.naming.NodeName;
import org.moera.lib.naming.types.OperationStatus;
import org.moera.lib.naming.types.OperationStatusInfo;
import org.moera.lib.naming.types.RegisteredNameInfo;
import org.moera.node.domain.Domains;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.global.UniversalContext;
import org.moera.node.liberin.LiberinManager;
import org.moera.node.liberin.model.NodeNameChangedLiberin;
import org.moera.node.liberin.model.RegisteredNameOperationStatusLiberin;
import org.moera.node.model.OperationFailure;
import org.moera.node.operations.SubscriptionOperations;
import org.moera.node.option.Options;
import org.moera.node.util.Transaction;
import org.moera.node.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

@Service
public class NamingClient {

    private static final Logger log = LoggerFactory.getLogger(NamingClient.class);

    private final Map<String, MoeraNaming> namingServices = new ConcurrentHashMap<>();

    @Inject
    private UniversalContext universalContext;

    @Inject
    private Domains domains;

    @Inject
    private SubscriptionOperations subscriptionOperations;

    @Inject
    private TaskScheduler taskScheduler;

    @Inject
    private Transaction tx;

    @Inject
    private LiberinManager liberinManager;

    @EventListener(DomainsConfiguredEvent.class)
    public void optionsLoaded() {
        domains.getAllDomainNames().stream().map(domains::getDomainOptions).forEach(this::monitorOperation);
    }

    private MoeraNaming getNamingService(Options options) {
        return getNamingService(options.getString("naming.location"));
    }

    private MoeraNaming getNamingService(String location) {
        return namingServices.computeIfAbsent(location, MoeraNaming::new);
    }

    private void monitorOperation(Options options) {
        MDC.put("domain", domains.getDomainName(options.nodeId()));
        UUID operationId = options.getUuid("naming.operation.id");
        if (operationId == null) {
            log.debug("No pending naming operation");
            return;
        }
        MoeraNaming namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return;
        }
        log.info("Started monitoring for naming operation {}", operationId);
        updateOperationStatus(options, OperationStatus.WAITING);
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
                    unknownOperationStatus(options);
                } else {
                    log.info("Naming service unavailable, retry {} of {}...", n, maxRetries);
                }
                return;
            }

            tx.executeWriteQuietly(() -> {
                retries.set(0);
                if (info.getStatus() == null) {
                    unknownOperationStatus(options);
                }
                log.info("Naming operation {}, status is {}", id, info.getStatus().name());
                updateOperationStatus(options, info.getStatus());
                options.set("naming.operation.added", info.getAdded());
                options.set("naming.operation.status.updated", Util.now());
                switch (info.getStatus()) {
                    case ADDED:
                    case STARTED:
                        break;
                    case SUCCEEDED:
                        options.set("naming.operation.completed", info.getCompleted());
                        commitOperation(options);
                        options.reset("naming.operation.id");
                        break;
                    case FAILED:
                        options.set("naming.operation.completed", info.getCompleted());
                        options.set("naming.operation.error-code", "naming." + info.getErrorCode());
                        options.set("naming.operation.error-message", info.getErrorMessage());
                        options.reset("naming.operation.id");
                        break;
                }
            });

            if (options.getUuid("naming.operation.id") == null) {
                log.info("Stopped monitoring naming operation {}", id);
            }
        }, context -> {
            if (options.getUuid("naming.operation.id") == null) {
                return null;
            }
            Instant last = context.lastCompletion();
            return last == null ? Instant.now() : last.plusSeconds(10);
        });
    }

    private void updateOperationStatus(Options options, OperationStatus status) {
        String prevStatus = options.getString("naming.operation.status");
        options.set("naming.operation.status", status.getValue());
        if (!Objects.equals(prevStatus, status.getValue())) {
            liberinManager.send(new RegisteredNameOperationStatusLiberin().withNodeId(options.nodeId()));
        }
    }

    private void unknownOperationStatus(Options options) {
        log.info("Status of naming operation {} is set to 'unknown'", options.getString("naming.operation.id"));

        updateOperationStatus(options, OperationStatus.UNKNOWN);
        options.set("naming.operation.status.updated", Util.now());
        options.set("naming.operation.error-code", "naming.unknown");
        options.set("naming.operation.error-message", "operation status is unknown");
    }

    private void operationSent(UUID operationId, Options options) {
        log.info("Created naming operation {}", operationId);

        options.set("naming.operation.id", operationId);
        updateOperationStatus(options, OperationStatus.WAITING);
        options.set("naming.operation.status.updated", Util.now());
        options.reset("naming.operation.error-code");
        options.reset("naming.operation.error-message");
        options.reset("naming.operation.completed");
        options.reset("naming.operation.registered-name");
        options.reset("naming.operation.signing-key");
    }

    private void commitOperation(Options options) {
        String newRegisteredName = options.getString("naming.operation.registered-name");
        String prevRegisteredName = options.nodeName();
        options.set("profile.node-name", newRegisteredName);
        if (!Objects.equals(prevRegisteredName, newRegisteredName)) {
            liberinManager.send(
                new NodeNameChangedLiberin(newRegisteredName, prevRegisteredName, options, null)
                    .withNodeId(options.nodeId())
            );
        }

        PrivateKey signingKey = options.getPrivateKey("naming.operation.signing-key");
        if (signingKey != null) {
            options.set("profile.signing-key", signingKey);
        }

        universalContext.associate(options.nodeId());
        try {
            subscriptionOperations.autoSubscribe();
        } catch (Throwable e) {
            log.error("Error automatically subscribing the node", e);
        }
    }

    public RegisteredNameInfo getCurrent(String name, int generation, String namingLocation) {
        MoeraNaming namingService = getNamingService(namingLocation);
        if (namingService == null) {
            log.error("No naming service available");
            return null;
        }
        return namingService.getCurrent(name, generation);
    }

    public RegisteredNameInfo getPast(String name, int generation, long at, String namingLocation) {
        MoeraNaming namingService = getNamingService(namingLocation);
        if (namingService == null) {
            log.error("No naming service available");
            return null;
        }
        return namingService.getPast(name, generation, at);
    }

    public void register(
        String name,
        String nodeUri,
        ECPublicKey updatingKey,
        ECPrivateKey privateSigningKey,
        ECPublicKey signingKey,
        Options options
    ) {
        MoeraNaming namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return;
        }

        byte[] updatingKeyR = CryptoUtil.rawPublicKey(updatingKey);
        byte[] signingKeyR = CryptoUtil.rawPublicKey(signingKey);
        long validFrom = Instant.now()
            .plus(options.getDuration("profile.signing-key.valid-from.layover").getDuration())
            .getEpochSecond();
        log.info(
            "Registering name '{}': node uri = {}, updating key = {}, signing key = {}, valid from = {}",
            name, nodeUri, Util.dump(updatingKeyR), Util.dump(signingKeyR), Util.formatTimestamp(validFrom)
        );
        UUID operationId;
        NodeName registeredName = NodeName.parse(name);
        try {
            RegisteredNameInfo info = namingService.getCurrent(registeredName.getName(), registeredName.getGeneration());
            byte[] previousDigest = info != null ? info.getDigest() : null;
            operationId = namingService.put(
                registeredName.getName(),
                registeredName.getGeneration(),
                updatingKeyR,
                nodeUri,
                signingKeyR,
                validFrom,
                previousDigest,
                null
            );
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        operationSent(operationId, options);
        options.set("naming.operation.registered-name", registeredName.toString());
        options.set("naming.operation.signing-key", privateSigningKey);
        monitorOperation(options);
    }

    public void update(
        String name,
        String nodeUri,
        ECPrivateKey privateUpdatingKey,
        ECPrivateKey privateSigningKey,
        ECPublicKey signingKey,
        Options options
    ) {
        MoeraNaming namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return;
        }

        NodeName registeredName = NodeName.parse(name);
        RegisteredNameInfo info;
        try {
            info = namingService.getCurrent(registeredName.getName(), registeredName.getGeneration());
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        if (info == null) {
            throw new OperationFailure("name-not-registered");
        }
        // TODO possible to validate the private key by the public key
        log.info("Updating name '{}'", name);

        byte[] previousDigest = info.getDigest();
        log.info("Previous digest is {}", previousDigest != null ? Util.dump(previousDigest) : "null");
        nodeUri = nodeUri != null ? nodeUri : info.getNodeUri();
        byte[] signingKeyR = signingKey != null ? CryptoUtil.rawPublicKey(signingKey) : info.getSigningKey();
        long validFrom = signingKey != null
            ? Instant.now()
                .plus(options.getDuration("profile.signing-key.valid-from.layover").getDuration())
                .getEpochSecond()
            : info.getValidFrom();
        byte[] putCall = Fingerprints.putCall(
            info.getName(),
            info.getGeneration(),
            info.getUpdatingKey(),
            nodeUri,
            signingKeyR,
            Util.toTimestamp(validFrom),
            info.getDigest()
        );

        UUID operationId;

        if (log.isDebugEnabled()) {
            log.debug("Data to be signed: {}", Util.dump(putCall));
        }
        byte[] signature = CryptoUtil.sign(putCall, privateUpdatingKey);

        try {
            operationId = namingService.put(
                info.getName(),
                info.getGeneration(),
                null,
                nodeUri,
                signingKey != null ? signingKeyR : null,
                signingKey != null ? validFrom : null,
                info.getDigest(),
                signature
            );
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        operationSent(operationId, options);
        options.set("naming.operation.registered-name", registeredName.toString());
        if (privateSigningKey != null) {
            options.set("naming.operation.signing-key", privateSigningKey);
        }
        monitorOperation(options);
    }

}
