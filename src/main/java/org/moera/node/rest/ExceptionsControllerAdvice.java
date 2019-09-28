package org.moera.node.rest;

import java.util.Locale;
import javax.inject.Inject;

import org.moera.node.global.ApiController;
import org.moera.node.global.AuthenticationException;
import org.moera.node.global.InvalidTokenException;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.Result;
import org.moera.node.model.ValidationFailure;
import org.moera.node.naming.NamingNotAvailableException;
import org.moera.node.option.OptionValueException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = ApiController.class)
public class ExceptionsControllerAdvice {

    private static Logger log = LoggerFactory.getLogger(ExceptionsControllerAdvice.class);

    @Inject
    private MessageSource messageSource;

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result exception(Throwable e) {
        log.error("Exception in controller", e);

        String errorCode = "server.misconfiguration";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message + ": " + e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result invalidSyntax(HttpMessageConversionException e) {
        String errorCode = "invalid-syntax";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result validation(MethodArgumentNotValidException e) {
        ObjectError objectError = e.getBindingResult().getAllErrors().get(0);
        String errorCode = objectError.getCodes() != null && objectError.getCodes().length > 0
                ? objectError.getCodes()[0] : "";
        String message = messageSource.getMessage(objectError, Locale.getDefault());
        return new Result(errorCode.toLowerCase(), message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result typeMismatch(MethodArgumentTypeMismatchException e) {
        String errorCode = "invalid-argument-value";
        String message = messageSource.getMessage(errorCode, new Object[]{e.getName()},
                Locale.getDefault());
        return new Result(errorCode, message);

    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result objectNotFound(ObjectNotFoundFailure e) {
        String message = messageSource.getMessage(e, Locale.getDefault());
        return new Result(e.getErrorCode(), message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result validationFailure(ValidationFailure e) {
        String message = messageSource.getMessage(e, Locale.getDefault());
        return new Result(e.getErrorCode(), message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.CONFLICT)
    public Result operationFailure(OperationFailure e) {
        String message = messageSource.getMessage(e, Locale.getDefault());
        return new Result(e.getErrorCode(), message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result optionValueException(OptionValueException e) {
        String message = messageSource.getMessage(e, Locale.getDefault());
        return new Result(e.getErrorCode(), message + ": " + e.getMessage());
    }

    @ExceptionHandler
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result namingFailure(NamingNotAvailableException e) {
        String errorCode = "naming.not-available";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result authenticationRequired(AuthenticationException e) {
        String errorCode = "authentication.required";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result authenticationInvalid(InvalidTokenException e) {
        String errorCode = "authentication.invalid";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

}
