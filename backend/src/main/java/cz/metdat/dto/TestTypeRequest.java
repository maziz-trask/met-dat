package cz.metdat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TestTypeRequest(
    @NotBlank(message = "{validation.type.name.required}") @Size(max = 150, message = "{validation.type.name.size}") String name,
    @Size(max = 1000, message = "{validation.type.description.size}") String description,
    Boolean active) {}
