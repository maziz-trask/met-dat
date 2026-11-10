package cz.metdat.exception;

import java.time.Instant;
import java.util.List;

/** Uniform error body for every failing API call. */
public record ApiError(Instant timestamp, int status, String code, String message, String path, List<FieldIssue> fieldErrors) {
    public record FieldIssue(String field, String message) {}
}
