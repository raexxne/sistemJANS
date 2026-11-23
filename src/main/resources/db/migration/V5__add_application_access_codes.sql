CREATE TABLE application_access_codes (
    application_id BIGINT NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    failed_attempts INT NOT NULL DEFAULT 0,
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (application_id),
    CONSTRAINT fk_application_access_codes_application
        FOREIGN KEY (application_id) REFERENCES applications (id) ON DELETE CASCADE
);
