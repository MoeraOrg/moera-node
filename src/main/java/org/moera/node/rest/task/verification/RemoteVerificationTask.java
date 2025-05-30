package org.moera.node.rest.task.verification;

import java.util.Locale;
import jakarta.inject.Inject;

import org.moera.lib.node.exception.MoeraNodeApiException;
import org.moera.lib.node.exception.MoeraNodeApiNotFoundException;
import org.moera.node.api.node.MoeraNodeUnknownNameException;
import org.moera.node.task.Task;
import org.springframework.context.MessageSource;

public abstract class RemoteVerificationTask extends Task {

    @Inject
    private MessageSource messageSource;

    public RemoteVerificationTask() {
    }

    protected void error(Throwable e) {
        if (e instanceof MoeraNodeUnknownNameException) {
            failed("remote-node.not-found", null);
        } else if (e instanceof MoeraNodeApiException) {
            if (e instanceof MoeraNodeApiNotFoundException) {
                failed("remote-node.object-not-found", null);
            } else {
                failed("remote-node.internal-error", null);
            }
        } else {
            failed("remote-node.invalid-answer", e.getMessage());
        }
    }

    protected final void succeeded(boolean correct) {
        reportSuccess(correct);
    }

    protected abstract void reportSuccess(boolean correct);

    protected final void failed(String errorCode, String message) {
        String errorMessage = messageSource.getMessage(errorCode, null, Locale.getDefault());
        if (message != null) {
            errorMessage += ": " + message;
        }
        reportFailure(errorCode, errorMessage);
    }

    protected abstract void reportFailure(String errorCode, String errorMessage);

}
