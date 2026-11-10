package cz.metdat.exception;

/** The requested resource does not exist -> HTTP 404. */
public class NotFoundException extends AppException {
    public NotFoundException(String key, Object... args) { super(key, args); }
}
