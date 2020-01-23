package org.moera.node.rest.task;

import java.util.Locale;
import java.util.UUID;
import javax.inject.Inject;

import org.moera.naming.rpc.RegisteredNameInfo;
import org.moera.node.naming.NamingClient;
import org.moera.node.naming.NodeName;
import org.moera.node.naming.RegisteredName;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public abstract class RemoteVerificationTask extends RemoteTask {

    @Inject
    private NamingClient namingClient;

    @Inject
    private MessageSource messageSource;

    public RemoteVerificationTask(UUID nodeId) {
        super(nodeId);
    }

    protected byte[] fetchSigningKey(String ownerName, long at) {
        String namingLocation = domains.getDomainOptions(nodeId).getString("naming.location");
        RegisteredName registeredName = (RegisteredName) NodeName.parse(ownerName);
        RegisteredNameInfo nameInfo =
                namingClient.getPast(registeredName.getName(), registeredName.getGeneration(), at, namingLocation);
        return nameInfo != null ? nameInfo.getSigningKey() : null;
    }

    protected void error(Throwable e) {
        if (e instanceof WebClientResponseException) {
            if (((WebClientResponseException) e).getStatusCode() == HttpStatus.NOT_FOUND) {
                failed("remote-node.object-not-found", null);
            } else {
                failed("remote-node.internal-error", null);
            }
        } else {
            failed("remote-node.invalid-answer", e.getMessage());
        }
    }

    protected final void succeeded(boolean correct) {
        initLoggingDomain();
        reportSuccess(correct);
    }

    protected abstract void reportSuccess(boolean correct);

    protected final void failed(String errorCode, String message) {
        initLoggingDomain();
        String errorMessage = messageSource.getMessage(errorCode, null, Locale.getDefault());
        if (message != null) {
            errorMessage += ": " + message;
        }
        reportFailure(errorCode, errorMessage);
    }

    protected abstract void reportFailure(String errorCode, String errorMessage);

}
