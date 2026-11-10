package cz.metdat.dto;

/** Tells the sign-in page which methods this deployment offers. */
public record AuthConfig(boolean demoEnabled, boolean registrationEnabled) {}
