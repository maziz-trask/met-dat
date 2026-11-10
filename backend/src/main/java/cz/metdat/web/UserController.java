package cz.metdat.web;

import cz.metdat.dto.RoleChangeRequest;
import cz.metdat.dto.UserSummary;
import cz.metdat.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users (Editor only)")
public class UserController {
    private final UserService service;

    public UserController(UserService service) { this.service = service; }

    @GetMapping
    @PreAuthorize("hasRole('EDITOR')")
    public List<UserSummary> list() { return service.list(); }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('EDITOR')")
    public UserSummary changeRole(@PathVariable Long id, @Valid @RequestBody RoleChangeRequest req, @AuthenticationPrincipal Jwt token) {
        return service.changeRole(id, req.role(), token.getSubject());
    }
}
