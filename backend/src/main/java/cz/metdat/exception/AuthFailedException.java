package cz.metdat.exception;

/** Sign-in failed or is temporarily blocked -> HTTP 401 / 429. */
public class AuthFailedException extends AppException {
    private final boolean tooManyAttempts;

    public AuthFailedException(String key, boolean tooManyAttempts) {
        super(key);
        this.tooManyAttempts = tooManyAttempts;
    }

    public boolean isTooManyAttempts() { return tooManyAttempts; }
}
