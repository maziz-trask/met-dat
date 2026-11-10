package cz.metdat.exception;

/** Semantic validation failure of a single field -> HTTP 400. */
public class ValidationException extends RuntimeException {
    private final String field;
    public ValidationException(String field, String message) { super(message); this.field = field; }
    public String getField() { return field; }
}
