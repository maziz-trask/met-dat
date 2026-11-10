package cz.metdat.repository;

import cz.metdat.domain.AppUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    List<AppUser> findAllByActiveTrueOrderByRoleAscNameAsc();
    Optional<AppUser> findByEmailIgnoreCase(String email);
}
