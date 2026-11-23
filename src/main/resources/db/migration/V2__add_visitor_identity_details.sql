ALTER TABLE application_visitors
    MODIFY no_kad_pengenalan VARCHAR(255) NULL,
    ADD COLUMN jenis_pengenalan_diri VARCHAR(30) NULL,
    ADD COLUMN warganegara VARCHAR(255) NULL,
    ADD COLUMN no_permit_kerja VARCHAR(255) NULL;
