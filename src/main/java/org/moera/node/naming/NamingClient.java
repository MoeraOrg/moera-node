package org.moera.node.naming;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import javax.inject.Inject;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import org.moera.commons.crypto.CryptoUtil;
import org.moera.commons.util.Util;
import org.moera.naming.rpc.NamingService;
import org.moera.naming.rpc.OperationStatus;
import org.moera.naming.rpc.OperationStatusInfo;
import org.moera.naming.rpc.PutCallFingerprint;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.domain.Domains;
import org.moera.node.domain.DomainsConfiguredEvent;
import org.moera.node.event.EventManager;
import org.moera.node.event.model.NodeNameChangedEvent;
import org.moera.node.event.model.RegisteredNameOperationStatusEvent;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
public class NamingClient {

    private static Logger log = LoggerFactory.getLogger(NamingClient.class);

    private Map<String, NamingService> namingServices = new HashMap<>();

    @Inject
    private Domains domains;

    @Inject
    private TaskScheduler taskScheduler;

    @Inject
    private PlatformTransactionManager txManager;

    @Inject
    private EventManager eventManager;

    @EventListener(DomainsConfiguredEvent.class)
    public void optionsLoaded() {
        domains.getAllDomainNames().stream().map(domains::getDomainOptions).forEach(this::monitorOperation);
    }

    private NamingService getNamingService(Options options) {
        return namingServices.computeIfAbsent(options.getString("naming.location"), location -> {
            try {
                JsonRpcHttpClient client = new JsonRpcHttpClient(new URL(location));
                return ProxyUtil.createClientProxy(getClass().getClassLoader(), NamingService.class, client);
            } catch (MalformedURLException e) {
                log.error(e.getMessage());
                return null;
            }
        });
    }

    private void monitorOperation(Options options) {
        UUID operationId = options.getUuid("naming.operation.id");
        if (operationId == null) {
            log.info("No pending naming operation");
            return;
        }
        NamingService namingService = getNamingService(options);
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

            final TransactionStatus ts = txManager.getTransaction(new DefaultTransactionDefinition());
            try {
                retries.set(0);
                if (info.getStatus() == null) {
                    unknownOperationStatus(options);
                    return;
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
                txManager.commit(ts);
            } catch (Exception e) {
                txManager.rollback(ts);
                throw e;
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

    private void updateOperationStatus(Options options, OperationStatus status) {
        String prevStatus = options.getString("naming.operation.status");
        options.set("naming.operation.status", status.getValue());
        if (!Objects.equals(prevStatus, status.getValue())) {
            eventManager.send(options.nodeId(), new RegisteredNameOperationStatusEvent());
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
        options.reset("naming.operation.registered-name.generation");
        options.reset("naming.operation.signing-key");
    }

    private void commitOperation(Options options) {
        String name = options.getString("naming.operation.registered-name");
        Integer generation = options.getInt("naming.operation.registered-name.generation");
        String prevRegisteredName = options.getString("profile.node-name");
        String newRegisteredName = new RegisteredName(name, generation).toString();
        options.set("profile.node-name", newRegisteredName);
        if (!Objects.equals(prevRegisteredName, newRegisteredName)) {
            eventManager.send(options.nodeId(), new NodeNameChangedEvent(newRegisteredName));
        }

        PrivateKey signingKey = options.getPrivateKey("naming.operation.signing-key");
        if (signingKey != null) {
            options.set("profile.signing-key", signingKey);
        }
    }

    public RegisteredNameInfo getCurrent(String name, int generation, Options options) {
        NamingService namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return null;
        }
        return namingService.getCurrent(name, generation);
    }

    public RegisteredNameInfo getCurrentForLatest(String name, Options options) {
        NamingService namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return null;
        }
        return namingService.getCurrentForLatest(name);
    }

    public RegisteredNameInfo getPast(String name, int generation, long at, Options options) {
        NamingService namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return null;
        }
        return namingService.getPast(name, generation, at);
    }

    public RegisteredNameInfo getPastForLatest(String name, long at, Options options) {
        NamingService namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return null;
        }
        return namingService.getPastForLatest(name, at);
    }

    public void register(String name, String nodeUri, ECPublicKey updatingKey,
                         ECPrivateKey privateSigningKey, ECPublicKey signingKey, Options options) {

        NamingService namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return;
        }

        byte[] updatingKeyR = CryptoUtil.toRawPublicKey(updatingKey);
        byte[] signingKeyR = CryptoUtil.toRawPublicKey(signingKey);
        long validFrom = Instant.now()
                                .plus(options.getDuration("profile.signing-key.valid-from.layover"))
                                .getEpochSecond();
        log.info("Registering name '{}': node uri = {}, updating key = {}, signing key = {}, valid from = {}",
                name, nodeUri, Util.dump(updatingKeyR), Util.dump(signingKeyR), Util.formatTimestamp(validFrom));
        UUID operationId;
        int generation;
        try {
            RegisteredNameInfo info = namingService.getCurrentForLatest(name);
            generation = info != null ? info.getGeneration() + 1 : 0;
            byte[] previousDigest = info != null ? info.getDigest() : null;
            operationId = namingService.put(
                    name,
                    generation,
                    updatingKeyR,
                    nodeUri,
                    signingKeyR,
                    validFrom,
                    previousDigest,
                    null);
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        operationSent(operationId, options);
        options.set("naming.operation.registered-name", name);
        options.set("naming.operation.registered-name.generation", generation);
        options.set("naming.operation.signing-key", privateSigningKey);
        monitorOperation(options);
    }

    public void update(String name, int generation, ECPrivateKey privateUpdatingKey,
                       ECPrivateKey privateSigningKey, ECPublicKey signingKey, Options options) {

        NamingService namingService = getNamingService(options);
        if (namingService == null) {
            log.error("No naming service available");
            return;
        }

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

        byte[] previousDigest = info.getDigest();
        log.info("Previous digest is {}", previousDigest != null ? Util.dump(previousDigest) : "null");
        byte[] signingKeyR = signingKey != null ? CryptoUtil.toRawPublicKey(signingKey) : info.getSigningKey();
        long validFrom = signingKey != null
                ? Instant.now()
                    .plus(options.getDuration("profile.signing-key.valid-from.layover"))
                    .getEpochSecond()
                : info.getValidFrom();
        Object putCall = new PutCallFingerprint(
                info.getName(),
                info.getGeneration(),
                info.getUpdatingKey(),
                info.getNodeUri(),
                signingKeyR,
                validFrom,
                info.getDigest());

        UUID operationId;

        if (log.isDebugEnabled()) {
            log.debug("Data to be signed: {}", Util.dump(CryptoUtil.fingerprint(putCall)));
        }
        byte[] signature = CryptoUtil.sign(putCall, privateUpdatingKey);

        try {
            operationId = namingService.put(
                    name,
                    info.getGeneration(),
                    null,
                    null,
                    signingKey != null ? signingKeyR : null,
                    signingKey != null ? validFrom : null,
                    info.getDigest(),
                    signature);
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        operationSent(operationId, options);
        options.set("naming.operation.registered-name", name);
        options.set("naming.operation.registered-name.generation", generation);
        if (privateSigningKey != null) {
            options.set("naming.operation.signing-key", privateSigningKey);
        }
        monitorOperation(options);
    }

}
