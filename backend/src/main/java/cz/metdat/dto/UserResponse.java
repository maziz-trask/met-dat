package cz.metdat.dto;

public record UserResponse(Long id, String code, String name, String email, String role, String department) {}
