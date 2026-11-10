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

    public String getCode() { return id == null ? null : "U%03d".formatted(id); }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public String getDepartment() { return department; }
    public boolean isActive() { return active; }
}
