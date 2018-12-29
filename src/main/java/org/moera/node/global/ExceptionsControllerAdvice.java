package org.moera.node.global;

import java.util.Locale;
import javax.inject.Inject;

import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionsControllerAdvice {

    private static Logger log = LoggerFactory.getLogger(ExceptionsControllerAdvice.class);

    @Inject
    private MessageSource messageSource;

    @ExceptionHandler
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result exception(Throwable e) {
        log.error("Exception in controller", e);

        String errorCode = "server.misconfiguration";
        String message = null;
        try {
            message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        } catch (NoSuchMessageException me) {
            message = messageSource.getMessage("no-message", null, Locale.getDefault());
        }
        return new Result(errorCode, message + ": " + e.getMessage());
    }

    @ExceptionHandler
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result validation(MethodArgumentNotValidException e) {
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        String errorCode = objectError.getCodes() != null && objectError.getCodes().length > 0
                ? objectError.getCodes()[0] : "";
        String message = messageSource.getMessage(objectError, Locale.getDefault());
        return new Result(errorCode.toLowerCase(), message);
    }

    @ExceptionHandler
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result failure(OperationFailure e) {
        String message = messageSource.getMessage(e, Locale.getDefault());
        return new Result(e.getErrorCode(), message);
    }

}
