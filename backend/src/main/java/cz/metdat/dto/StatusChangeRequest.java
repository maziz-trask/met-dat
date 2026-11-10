package cz.metdat.dto;

import cz.metdat.domain.TestStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(@NotNull(message = "{validation.status.required}") TestStatus status) {}
