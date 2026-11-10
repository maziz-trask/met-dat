package cz.metdat.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Identity of the caller as verified by the server (JWT subject = e-mail). */
@Component
public class CurrentUser {
    public String email() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getName()) ? "system" : a.getName();
    }
}
