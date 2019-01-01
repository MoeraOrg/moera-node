package org.moera.node.naming;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
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
import org.moera.commons.util.SignatureDataBuilder;
import org.moera.commons.util.Util;
import org.moera.naming.rpc.NamingService;
import org.moera.naming.rpc.OperationStatusInfo;
import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.model.OperationFailure;
import org.moera.node.option.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        monitorOperation();
    }

    private void monitorOperation() {
        if (options.getUuid("naming.operation.id") == null) {
            return;
        }
        final AtomicInteger retries = new AtomicInteger(0);
        taskScheduler.schedule(() -> {
            UUID id = options.getUuid("naming.operation.id");
            if (id == null) {
                return;
            }
            OperationStatusInfo info;
            try {
                info = namingService.getStatus(id);
            } catch (Exception e) {
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
            options.set("naming.operation.status", info.getStatus().name().toLowerCase());
            options.set("naming.operation.added", info.getAdded());
            switch (info.getStatus()) {
                case ADDED:
                case STARTED:
                    break;
                case SUCCEEDED:
                    options.set("naming.operation.completed", info.getCompleted());
                    options.set("profile.registered-name.generation", info.getGeneration());
                    options.reset("naming.operation.id");
                    break;
                case FAILED:
                    options.set("naming.operation.completed", info.getCompleted());
                    options.set("naming.operation.error-code", "naming." + info.getErrorCode());
                    options.reset("naming.operation.id");
                    break;
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

        options.set("naming.operation.status", "unknown");
        options.set("naming.operation.error-code", "naming.unknown");
        options.set("naming.operation.completed", Util.now());
        options.reset("naming.operation.id");
    }

    public void register(String name, PublicKey updatingKey, PublicKey signingKey) {
        String updatingKeyE = Util.base64encode(CryptoUtil.toRawPublicKey(updatingKey));
        String signingKeyE = Util.base64encode(CryptoUtil.toRawPublicKey(signingKey));
        long validFrom = Instant.now()
                                .plus(options.getDuration("profile.signing-key.valid-from.layover"))
                                .getEpochSecond();
        UUID operationId;
        try {
            operationId = namingService.put(name, false, updatingKeyE, "", signingKeyE, validFrom, null);
        } catch (Exception e) {
            throw new NamingNotAvailableException(e);
        }
        options.set("naming.operation.id", operationId);
        options.set("profile.registered-name", name);
        monitorOperation();
    }

    public void update(String name, int generation, PrivateKey privateUpdatingKey) {
        RegisteredNameInfo info = namingService.getCurrent(name);
        if (info == null) {
            throw new OperationFailure("name-not-registered");
        }
        if (info.getGeneration() != generation) {
            throw new OperationFailure("wrong-generation");
        }
        // TODO possible to validate the private key by the public key

        UUID operationId;
        SignatureDataBuilder buf = new SignatureDataBuilder();
        try {
            buf.append(info.getName());
            buf.append(Util.base64decode(info.getUpdatingKey()));
            buf.append(info.getNodeUri());
            if (info.getSigningKey() != null) {
                buf.append(Util.base64decode(info.getSigningKey()));
                buf.append(info.getValidFrom());
            }

            Signature sign = Signature.getInstance("SHA3-256withECDSA", "BC");
            sign.initSign(privateUpdatingKey, SecureRandom.getInstanceStrong());
            sign.update(buf.toBytes());
            String signature = Util.base64encode(sign.sign());

            try {
                operationId = namingService.put(name, false, null, null, null, null, signature);
            } catch (Exception e) {
                throw new NamingNotAvailableException(e);
            }
        } catch (GeneralSecurityException | IOException e) {
            throw new CryptoException(e);
        }
        options.set("naming.operation.id", operationId);
        monitorOperation();
    }

}
