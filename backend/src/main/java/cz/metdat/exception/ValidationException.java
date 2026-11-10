package cz.metdat.exception;

/** Semantic validation failure of a single field -> HTTP 400. */
public class ValidationException extends AppException {
    private final String field;

    public ValidationException(String field, String key, Object... args) {
        super(key, args);
        this.field = field;
    }

    public String getField() { return field; }
}
