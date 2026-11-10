package cz.metdat.exception;

/** A business rule (e.g. F06) rejected the operation -> HTTP 409. */
public class BusinessRuleException extends RuntimeException {
    private final String code;
    public BusinessRuleException(String code, String message) { super(message); this.code = code; }
    public String getCode() { return code; }
}
