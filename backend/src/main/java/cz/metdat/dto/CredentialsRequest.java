package cz.metdat.dto;

import jakarta.validation.constraints.NotBlank;

public record CredentialsRequest(
    @NotBlank(message = "{validation.email.required}") String email,
    @NotBlank(message = "{validation.password.required}") String password) {}
