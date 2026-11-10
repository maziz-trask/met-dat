package cz.metdat.web;

import cz.metdat.exception.*;
import cz.metdat.exception.ApiError.FieldIssue;
import cz.metdat.i18n.Messages;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Turns every failure into the uniform, localised {@link ApiError} body. */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private final Messages messages;

    public GlobalExceptionHandler(Messages messages) { this.messages = messages; }

    private ResponseEntity<ApiError> body(HttpStatus status, String code, String message, HttpServletRequest req, List<FieldIssue> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code, message, req.getRequestURI(), fields));
    }

    private ResponseEntity<ApiError> body(HttpStatus status, String code, String messageKey, HttpServletRequest req, Object... args) {
        return body(status, code, messages.get(messageKey, args), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e, HttpServletRequest r) {
        List<FieldIssue> fields = e.getBindingResult().getFieldErrors().stream()
            .map(x -> new FieldIssue(x.getField(), x.getDefaultMessage())).distinct().toList();
        String joined = String.join("; ", fields.stream().map(FieldIssue::message).toList());
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", messages.get("error.validationFailed", joined), r, fields);
    }

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ApiError> validation(ValidationException e, HttpServletRequest r) {
        String message = messages.get(e);
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, r, List.of(new FieldIssue(e.getField(), message)));
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> rule(BusinessRuleException e, HttpServletRequest r) {
        return body(HttpStatus.CONFLICT, e.getCode(), messages.get(e), r, List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest r) {
        return body(HttpStatus.NOT_FOUND, "NOT_FOUND", messages.get(e), r, List.of());
    }

    @ExceptionHandler(AuthFailedException.class)
    ResponseEntity<ApiError> authFailed(AuthFailedException e, HttpServletRequest r) {
        HttpStatus status = e.isTooManyAttempts() ? HttpStatus.TOO_MANY_REQUESTS : HttpStatus.UNAUTHORIZED;
        return body(status, e.isTooManyAttempts() ? "TOO_MANY_ATTEMPTS" : "INVALID_CREDENTIALS", messages.get(e), r, List.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e, HttpServletRequest r) {
        for (Throwable t = e; t != null; t = t.getCause()) if (t instanceof ValidationException v) return validation(v, r);
        return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "error.malformed", r);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ResponseEntity<ApiError> badParam(Exception e, HttpServletRequest r) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "error.badParam", r, e.getMessage());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> locking(HttpServletRequest r) { return body(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "error.concurrent", r); }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        log.warn("Integrity violation", e);
        return body(HttpStatus.CONFLICT, "DATA_INTEGRITY", "error.conflictData", r);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(HttpServletRequest r) { return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "error.notFound", r); }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpServletRequest r) { return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "error.methodNotAllowed", r); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(HttpServletRequest r) { return body(HttpStatus.FORBIDDEN, "FORBIDDEN", "error.forbidden", r); }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(HttpServletRequest r) {
        String message = messages.get("error.fileTooLarge");
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", message, r, List.of(new FieldIssue("files", message)));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> other(Exception e, HttpServletRequest r) {
        log.error("Unhandled error on {}", r.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "error.internal", r);
    }
}
