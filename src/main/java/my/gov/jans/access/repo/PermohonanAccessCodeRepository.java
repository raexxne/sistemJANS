package my.gov.jans.access.repo;

import my.gov.jans.access.domain.PermohonanAccessCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface PermohonanAccessCodeRepository extends JpaRepository<PermohonanAccessCode, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from PermohonanAccessCode code where code.applicationId = :applicationId")
    Optional<PermohonanAccessCode> findByApplicationIdForUpdate(@Param("applicationId") Long applicationId);
}
