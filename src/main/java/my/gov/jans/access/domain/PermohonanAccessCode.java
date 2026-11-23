package my.gov.jans.access.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "application_access_codes")
public class PermohonanAccessCode {
    @Id
    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(nullable = false)
    private boolean verified;

    protected PermohonanAccessCode() {
    }

    public PermohonanAccessCode(Long applicationId, String codeHash, Instant expiresAt, Instant createdAt) {
        this.applicationId = applicationId;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public boolean isVerified() {
        return verified;
    }

    public void incrementFailedAttempts() {
        failedAttempts++;
    }

    public void markVerified() {
        verified = true;
    }
}
