package cz.metdat.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private Role role;
    private String department;
    @Column(nullable = false) private boolean active = true;
    /** BCrypt hash; NULL for the seeded demo identities, which can only sign in through demo login. */
    @Column(name = "password_hash") private String passwordHash;

    /** A newly self-registered account: always a Reader, never a role chosen by the caller. */
    public static AppUser register(String name, String email, String passwordHash) {
        AppUser u = new AppUser();
        u.name = name;
        u.email = email;
        u.passwordHash = passwordHash;
        u.role = Role.READER;
        u.active = true;
        return u;
    }

    public String getCode() { return id == null ? null : "U%03d".formatted(id); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getDepartment() { return department; }
    public boolean isActive() { return active; }
    public String getPasswordHash() { return passwordHash; }
    public boolean hasPassword() { return passwordHash != null; }
}
