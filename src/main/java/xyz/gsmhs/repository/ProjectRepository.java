package xyz.gsmhs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import xyz.gsmhs.domain.Project;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    boolean existsBySubdomainIgnoreCase(String subdomain);

    boolean existsBySubdomainIgnoreCaseAndIdNot(String subdomain, Long id);

    List<Project> findAllByOrderByCreatedAtDesc();
}
