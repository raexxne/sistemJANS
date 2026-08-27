package my.gov.jans.access.repo;

import my.gov.jans.access.domain.JenisLokasi;
import my.gov.jans.access.domain.Lokasi;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LokasRepository extends JpaRepository<Lokasi, Long> {
    List<Lokasi> findByType(JenisLokasi type);
    List<Lokasi> findByNameContainingIgnoreCase(String name);
}
