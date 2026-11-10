package cz.metdat.exception;

/**
 * Base of all expected (business/validation/lookup) failures. Carries a message KEY and arguments, never display text:
 * the text is resolved per request language (Accept-Language) in GlobalExceptionHandler from messages*.properties.
 */
public abstract class AppException extends RuntimeException {
    private final String key;
    private final transient Object[] args;

    protected AppException(String key, Object... args) {
        super(key);
        this.key = key;
        this.args = args;
    }

    public String getKey() { return key; }
    public Object[] getArgs() { return args; }
}
