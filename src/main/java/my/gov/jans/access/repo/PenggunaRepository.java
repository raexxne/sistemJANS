package my.gov.jans.access.repo;

import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.domain.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PenggunaRepository extends JpaRepository<Pengguna, Long> {
    Optional<Pengguna> findByEmail(String email);

    List<Pengguna> findByRole(Role role);
}
