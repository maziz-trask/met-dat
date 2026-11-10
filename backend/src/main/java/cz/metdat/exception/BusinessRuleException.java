package cz.metdat.exception;

/** A business rule (e.g. F06) rejected the operation -> HTTP 409. {@code code} is a stable machine-readable identifier. */
public class BusinessRuleException extends AppException {
    private final String code;

    public BusinessRuleException(String code, String key, Object... args) {
        super(key, args);
        this.code = code;
    }

    public String getCode() { return code; }
}
