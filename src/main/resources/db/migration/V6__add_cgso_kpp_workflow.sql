ALTER TABLE applications
    MODIFY COLUMN status ENUM(
        'DIHANTAR',
        'SEMAKAN_STAFF',
        'MENUNGGU_CGSO',
        'MENUNGGU_KPP',
        'KEMBALI_PETUGAS',
        'MENUNGGU_PENGARAH',
        'DILULUSKAN',
        'DITOLAK',
        'PAS_DIKELUARKAN',
        'SELESAI'
    ) NOT NULL;

ALTER TABLE applications
    ADD COLUMN cgso_note TEXT NULL,
    ADD COLUMN kpp_note TEXT NULL;
