package my.gov.jans.access.repo;

import my.gov.jans.access.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface PermohonanRepository extends JpaRepository<Permohonan, Long> {
    Optional<Permohonan> findByNomborPermohonan(String n);

    Optional<Permohonan> findByPassToken(String t);

    Optional<Permohonan> findFirstByNomborPermohonanStartingWithOrderByNomborPermohonanDesc(String prefix);

    List<Permohonan> findByStatusOrderByCreatedAtDesc(StatusPermohonan s);

    List<Permohonan> findAllByOrderByUpdatedAtDesc();
}
