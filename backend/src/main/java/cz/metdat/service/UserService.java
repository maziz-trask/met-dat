package cz.metdat.service;

import cz.metdat.domain.Role;
import cz.metdat.dto.UserSummary;
import cz.metdat.exception.BusinessRuleException;
import cz.metdat.exception.NotFoundException;
import cz.metdat.mapper.TestMapper;
import cz.metdat.repository.AppUserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Editor-only user administration: list accounts and grant or revoke the Editor role. */
@Service
public class UserService {
    private final AppUserRepository users;
    private final TestMapper mapper;

    public UserService(AppUserRepository users, TestMapper mapper) { this.users = users; this.mapper = mapper; }

    @Transactional(readOnly = true)
    public List<UserSummary> list() {
        return users.findAllByOrderByRoleAscNameAsc().stream().map(mapper::toUserSummary).toList();
    }

    @Transactional
    public UserSummary changeRole(Long id, Role role, String actingEmail) {
        var user = users.findById(id).orElseThrow(() -> new NotFoundException("user.notFound", String.valueOf(id)));
        if (user.getEmail().equalsIgnoreCase(actingEmail)) {
            throw new BusinessRuleException("SELF_ROLE_CHANGE", "user.selfRoleChange");   // keeps at least one Editor
        }
        user.setRole(role);
        return mapper.toUserSummary(user);
    }
}
