package my.gov.jans.access.repo;

import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.domain.PenyeliaLoji;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PenyeliaLojiRepository extends JpaRepository<PenyeliaLoji, Long> {
    Optional<PenyeliaLoji> findByPengguna(Pengguna pengguna);

    Optional<PenyeliaLoji> findByPengguna_Id(Long userId);

    void deleteByPengguna_Id(Long userId);
}
