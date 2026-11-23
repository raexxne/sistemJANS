ALTER TABLE users
    MODIFY COLUMN role ENUM(
        'STAFF',
        'PENGARAH',
        'ADMIN',
        'PENYELIA_LOJI',
        'CGSO',
        'KPP_CAWANGAN_PENGELUARAN'
    ) NOT NULL;
