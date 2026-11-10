package cz.metdat.dto;

import java.time.Instant;

public record TestTypeResponse(Long id, String code, String name, String description, boolean active, long usageCount,
                               String createdBy, Instant createdAt, String modifiedBy, Instant modifiedAt) {}
