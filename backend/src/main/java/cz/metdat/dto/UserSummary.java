package cz.metdat.dto;

/** User row of the Editor-only user administration; {@code passwordLogin} is false for demo-only identities. */
public record UserSummary(Long id, String code, String name, String email, String role, String department, boolean active, boolean passwordLogin) {}
