package org.moera.node.rest.task;

import java.util.Locale;
import javax.inject.Inject;

import org.moera.node.task.Task;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

public abstract class RemoteVerificationTask extends Task {

    @Inject
    private MessageSource messageSource;

    public RemoteVerificationTask() {
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
