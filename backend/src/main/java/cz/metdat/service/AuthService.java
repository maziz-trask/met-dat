package cz.metdat.service;

import cz.metdat.config.AppProperties;
import cz.metdat.domain.AppUser;
import cz.metdat.dto.*;
import cz.metdat.exception.AuthFailedException;
import cz.metdat.exception.BusinessRuleException;
import cz.metdat.exception.NotFoundException;
import cz.metdat.exception.ValidationException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.AppUserRepository;
import cz.metdat.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration, password sign-in and demo sign-in. Every path ends in a server-signed token; the role always comes from the database. */
@Service
public class AuthService {
    private static final String EMAIL_TAKEN = "EMAIL_TAKEN";

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptLimiter limiter;
    private final TestMapper mapper;
    private final AppProperties props;
    /** Compared against when the e-mail is unknown, so response time does not reveal which e-mails exist. */
    private final String dummyHash;

    public AuthService(AppUserRepository users, PasswordEncoder encoder, JwtService jwt, LoginAttemptLimiter limiter,
                       TestMapper mapper, AppProperties props) {
        this.users = users; this.encoder = encoder; this.jwt = jwt; this.limiter = limiter; this.mapper = mapper; this.props = props;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    public AuthConfig config() { return new AuthConfig(props.demo().enabled(), props.auth().registrationEnabled()); }

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (!props.auth().registrationEnabled()) throw new NotFoundException("auth.registrationDisabled");
        String email = req.email().trim().toLowerCase();
        if (users.findByEmailIgnoreCase(email).isPresent()) throw new BusinessRuleException(EMAIL_TAKEN, "auth.emailTaken");
        try {
            AppUser saved = users.saveAndFlush(AppUser.register(req.name().trim(), email, encoder.encode(req.password())));
            return issue(saved);
        } catch (DataIntegrityViolationException race) {   // two registrations with the same e-mail at once
            throw new BusinessRuleException(EMAIL_TAKEN, "auth.emailTaken");
        }
    }

    @Transactional(readOnly = true)
    public LoginResponse login(CredentialsRequest req) {
        String email = req.email().trim().toLowerCase();
        if (limiter.isBlocked(email)) throw new AuthFailedException("auth.tooManyAttempts", true);
        AppUser user = users.findByEmailIgnoreCase(email).filter(AppUser::isActive).filter(AppUser::hasPassword).orElse(null);
        boolean ok = encoder.matches(req.password(), user == null ? dummyHash : user.getPasswordHash()) && user != null;
        if (!ok) {
            limiter.recordFailure(email);
            throw new AuthFailedException("auth.invalidCredentials", false);
        }
        limiter.reset(email);
        return issue(user);
    }

    @Transactional(readOnly = true)
    public LoginResponse demoLogin(Long userId) {
        if (!props.demo().enabled()) throw new NotFoundException("auth.demoDisabled");
        AppUser user = users.findById(userId).filter(AppUser::isActive).orElseThrow(() -> new ValidationException("userId", "auth.userInvalid"));
        return issue(user);
    }

    private LoginResponse issue(AppUser user) {
        JwtService.Token token = jwt.issue(user);
        return new LoginResponse(token.value(), token.expiresAt(), mapper.toUser(user));
    }
}
