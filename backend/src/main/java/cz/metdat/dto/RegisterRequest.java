package cz.metdat.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-registration. There is intentionally no role field: new accounts are always Readers. */
public record RegisterRequest(
    @NotBlank(message = "{validation.name.required}") @Size(max = 150, message = "{validation.name.size}") String name,
    @NotBlank(message = "{validation.email.required}") @Email(message = "{validation.email.format}") @Size(max = 255, message = "{validation.email.format}") String email,
    @NotBlank(message = "{validation.password.required}") @Size(min = 8, max = 72, message = "{validation.password.size}") String password) {}
