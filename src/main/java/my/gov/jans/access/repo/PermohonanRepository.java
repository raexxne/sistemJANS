package my.gov.jans.access.repo;

import my.gov.jans.access.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface PermohonanRepository extends JpaRepository<Permohonan, Long> {
    Optional<Permohonan> findByNomborPermohonan(String n);

    Optional<Permohonan> findByPassToken(String t);

    Optional<Permohonan> findFirstByNomborPermohonanStartingWithOrderByNomborPermohonanDesc(String prefix);

    List<Permohonan> findByStatusOrderByCreatedAtDesc(StatusPermohonan s);

    List<Permohonan> findAllByOrderByUpdatedAtDesc();

    @Modifying
    @Query("update Permohonan p set p.reviewedBy = null where p.reviewedBy.id = :userId")
    int clearReviewedBy(@Param("userId") Long userId);

    @Modifying
    @Query("update Permohonan p set p.decidedBy = null where p.decidedBy.id = :userId")
    int clearDecidedBy(@Param("userId") Long userId);

    @Modifying
    @Query("update Permohonan p set p.completedBy = null where p.completedBy.id = :userId")
    int clearCompletedBy(@Param("userId") Long userId);
}
