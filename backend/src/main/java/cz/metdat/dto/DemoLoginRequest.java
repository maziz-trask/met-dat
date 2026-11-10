package cz.metdat.dto;

import jakarta.validation.constraints.NotNull;

public record DemoLoginRequest(@NotNull(message = "{validation.userId.required}") Long userId) {}
