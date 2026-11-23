UPDATE applications
SET status = 'DIHANTAR'
WHERE status IN ('MENUNGGU_CGSO', 'MENUNGGU_KPP', 'KEMBALI_PETUGAS');

ALTER TABLE applications
    MODIFY COLUMN status ENUM(
        'DIHANTAR',
        'SEMAKAN_STAFF',
        'MENUNGGU_PENGARAH',
        'DILULUSKAN',
        'DITOLAK',
        'PAS_DIKELUARKAN',
        'SELESAI'
    ) NOT NULL;
