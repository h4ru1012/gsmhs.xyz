package xyz.gsmhs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.gsmhs.domain.AppUser;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCase(String email);

    List<AppUser> findAllByOrderByLastLoginAtDesc();
}
