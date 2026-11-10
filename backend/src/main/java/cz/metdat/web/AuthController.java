package cz.metdat.web;

import cz.metdat.config.AppProperties;
import cz.metdat.domain.AppUser;
import cz.metdat.dto.Dtos.*;
import cz.metdat.exception.NotFoundException;
import cz.metdat.exception.ValidationException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.AppUserRepository;
import cz.metdat.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

/**
 * Simplified demo identity (documented deviation from full IAM): the evaluator picks one of the fixed seeded users;
 * the server looks the user up and signs a token containing the role stored in the database.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Demo identity")
public class AuthController {
    private final AppUserRepository users;
    private final JwtService jwt;
    private final TestMapper mapper;
    private final AppProperties props;

    public AuthController(AppUserRepository users, JwtService jwt, TestMapper mapper, AppProperties props) {
        this.users = users; this.jwt = jwt; this.mapper = mapper; this.props = props;
    }

    @GetMapping("/demo-users")
    @Operation(summary = "Fixed seeded demo identities that can be used to sign in (public)")
    public List<UserResponse> demoUsers() {
        requireDemo();
        return users.findAllByActiveTrueOrderByRoleAscNameAsc().stream().map(mapper::toUser).toList();
    }

    @PostMapping("/demo-login")
    @Operation(summary = "Demo sign-in / role switch: returns a server-signed token for the chosen seeded user")
    public LoginResponse login(@Valid @RequestBody LoginRequest req) {
        requireDemo();
        AppUser u = users.findById(req.userId()).filter(AppUser::isActive)
            .orElseThrow(() -> new ValidationException("userId", "Unknown or inactive demo user"));
        JwtService.Token t = jwt.issue(u);
        return new LoginResponse(t.value(), t.expiresAt(), mapper.toUser(u));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user, as verified by the server from the token")
    public UserResponse me(@AuthenticationPrincipal Jwt token) {
        return users.findByEmailIgnoreCase(token.getSubject()).map(mapper::toUser)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void requireDemo() {
        if (!props.demo().enabled()) throw new NotFoundException("Demo mode is disabled");
    }
}
