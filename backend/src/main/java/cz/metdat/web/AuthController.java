package cz.metdat.web;

import cz.metdat.dto.*;
import cz.metdat.exception.NotFoundException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.AppUserRepository;
import cz.metdat.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/** Sign-in endpoints. Passwords are BCrypt-hashed; demo identities exist so evaluators can switch Reader/Editor in one click. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {
    private final AuthService auth;
    private final AppUserRepository users;
    private final TestMapper mapper;

    public AuthController(AuthService auth, AppUserRepository users, TestMapper mapper) {
        this.auth = auth; this.users = users; this.mapper = mapper;
    }

    @GetMapping("/config")
    @Operation(summary = "Which sign-in methods are enabled (public)")
    public AuthConfig config() { return auth.config(); }

    @PostMapping("/register")
    @Operation(summary = "Register with e-mail and password. New accounts are always Readers")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(auth.register(req));
    }

    @PostMapping("/login")
    @Operation(summary = "Sign in with e-mail and password")
    public LoginResponse login(@Valid @RequestBody CredentialsRequest req) { return auth.login(req); }

    @GetMapping("/demo-users")
    @Operation(summary = "Fixed seeded demo identities that can be used to sign in (public, demo mode only)")
    public List<UserResponse> demoUsers() {
        if (!auth.config().demoEnabled()) throw new NotFoundException("auth.demoDisabled");
        return users.findAllByActiveTrueOrderByRoleAscNameAsc().stream().filter(u -> !u.hasPassword()).map(mapper::toUser).toList();
    }

    @PostMapping("/demo-login")
    @Operation(summary = "Demo sign-in / role switch: returns a server-signed token for the chosen seeded user")
    public LoginResponse demoLogin(@Valid @RequestBody DemoLoginRequest req) { return auth.demoLogin(req.userId()); }

    @GetMapping("/me")
    @Operation(summary = "Current user, as verified by the server")
    public UserResponse me(@AuthenticationPrincipal Jwt token) {
        return users.findByEmailIgnoreCase(token.getSubject()).map(mapper::toUser).orElseThrow(() -> new NotFoundException("user.notFound", token.getSubject()));
    }
}
