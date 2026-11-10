package cz.metdat.web;

import cz.metdat.exception.*;
import cz.metdat.exception.ApiError.FieldIssue;
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

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static ResponseEntity<ApiError> body(HttpStatus s, String code, String msg, HttpServletRequest req, List<FieldIssue> fields) {
        return ResponseEntity.status(s).body(new ApiError(Instant.now(), s.value(), code, msg, req.getRequestURI(), fields));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e, HttpServletRequest r) {
        List<FieldIssue> f = e.getBindingResult().getFieldErrors().stream().map(x -> new FieldIssue(x.getField(), x.getDefaultMessage())).distinct().toList();
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed: " + f.stream().map(FieldIssue::message).reduce((a, b) -> a + "; " + b).orElse(""), r, f);
    }

    @ExceptionHandler(ValidationException.class)
    ResponseEntity<ApiError> validation(ValidationException e, HttpServletRequest r) {
        return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", e.getMessage(), r, List.of(new FieldIssue(e.getField(), e.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e, HttpServletRequest r) {
        for (Throwable t = e; t != null; t = t.getCause()) if (t instanceof ValidationException v) return validation(v, r);
        return body(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "The request body is malformed or contains an invalid value.", r, List.of());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    ResponseEntity<ApiError> badParam(Exception e, HttpServletRequest r) {
        return body(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Invalid or missing request parameter: " + e.getMessage(), r, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    ResponseEntity<ApiError> rule(BusinessRuleException e, HttpServletRequest r) {
        return body(HttpStatus.CONFLICT, e.getCode(), e.getMessage(), r, List.of());
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<ApiError> locking(HttpServletRequest r) {
        return body(HttpStatus.CONFLICT, "CONCURRENT_MODIFICATION", "The record was changed by someone else. Reload it and try again.", r, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> integrity(DataIntegrityViolationException e, HttpServletRequest r) {
        log.warn("Integrity violation", e);
        return body(HttpStatus.CONFLICT, "DATA_INTEGRITY", "The operation conflicts with existing data.", r, List.of());
    }

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiError> notFound(NotFoundException e, HttpServletRequest r) { return body(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), r, List.of()); }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> noResource(HttpServletRequest r) { return body(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found.", r, List.of()); }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> method(HttpServletRequest r) { return body(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "HTTP method not supported.", r, List.of()); }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(HttpServletRequest r) {
        return body(HttpStatus.FORBIDDEN, "FORBIDDEN", "Your role is not allowed to perform this action.", r, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooLarge(HttpServletRequest r) {
        return body(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "The uploaded file(s) exceed the maximum allowed size.", r, List.of(new FieldIssue("files", "File too large")));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> other(Exception e, HttpServletRequest r) {
        log.error("Unhandled error on {}", r.getRequestURI(), e);
        return body(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "An unexpected error occurred.", r, List.of());
    }
}
