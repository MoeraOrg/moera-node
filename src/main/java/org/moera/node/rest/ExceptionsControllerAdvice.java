package org.moera.node.rest;

import java.util.Locale;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletResponse;

import org.moera.lib.node.types.Result;
import org.moera.lib.node.types.body.BodyMappingException;
import org.moera.lib.node.types.validate.ValidationFailure;
import org.moera.node.api.naming.NamingNotAvailableException;
import org.moera.node.auth.AuthenticationException;
import org.moera.node.auth.IncorrectSignatureException;
import org.moera.node.auth.InvalidCarteException;
import org.moera.node.auth.UserBlockedException;
import org.moera.node.global.ApiController;
import org.moera.node.global.PageNotFoundException;
import org.moera.node.model.ObjectNotFoundFailure;
import org.moera.node.model.OperationFailure;
import org.moera.node.model.TooManyRequestsFailure;
import org.moera.node.option.exception.OptionValueException;
import org.moera.node.plugin.PluginInvocationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(annotations = ApiController.class)
public class ExceptionsControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(ExceptionsControllerAdvice.class);

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
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result exception(PluginInvocationException e) {
        String errorCode = "plugin.invocation-error";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message + ": " + e.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result invalidSyntax(HttpMediaTypeNotSupportedException e) {
        String errorCode = "invalid-content-type";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
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
    public Result missing(MissingServletRequestParameterException e) {
        String errorCode = "missing-argument";
        String message = messageSource.getMessage(errorCode, new Object[]{e.getParameterName()},
                Locale.getDefault());
        return new Result(errorCode, message);

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
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result pageNotFound(PageNotFoundException e) {
        return new Result("not-found", "Page not found");
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Result tooManyRequests(TooManyRequestsFailure e, HttpServletResponse response) {
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(e.getRetryAfter()));
        response.setHeader("RateLimit-Policy", String.format("q=%d;w=%d", e.getLimit(), e.getPeriod()));
        String errorCode = "too-many-requests";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result validationException(ValidationFailure e) {
        String message = messageSource.getMessage(e.getErrorCode(), null, Locale.getDefault());
        return new Result(e.getErrorCode(), message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result bodyMappingException(BodyMappingException e) {
        String errorCode = "invalid-syntax";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
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
    public Result authenticationRequired(AuthenticationException e, HttpServletResponse response) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Node\"");
        String errorCode = "authentication.required";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result incorrectSignature(IncorrectSignatureException e, HttpServletResponse response) {
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer realm=\"Node\"");
        String errorCode = "authentication.incorrect-signature";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result carteInvalid(InvalidCarteException e) {
        String message = messageSource.getMessage(e.getErrorCode(), null, Locale.getDefault());
        return new Result(e.getErrorCode(), message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Result userBlocked(UserBlockedException e, HttpServletResponse response) {
        String errorCode = "authentication.blocked";
        String message = messageSource.getMessage(errorCode, null, Locale.getDefault());
        return new Result(errorCode, message);
    }

}
