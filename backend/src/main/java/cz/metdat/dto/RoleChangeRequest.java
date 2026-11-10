package cz.metdat.dto;

import cz.metdat.domain.Role;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(@NotNull(message = "{validation.role.required}") Role role) {}
