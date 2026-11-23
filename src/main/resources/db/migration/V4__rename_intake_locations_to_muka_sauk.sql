UPDATE lokasi
SET name = REPLACE(REPLACE(REPLACE(name, 'Intake', 'Muka Sauk'), 'intake', 'Muka Sauk'), 'INTAKE', 'Muka Sauk')
WHERE LOWER(name) LIKE '%intake%';

UPDATE applications
SET location_name = REPLACE(REPLACE(REPLACE(location_name, 'Intake', 'Muka Sauk'), 'intake', 'Muka Sauk'), 'INTAKE', 'Muka Sauk')
WHERE LOWER(location_name) LIKE '%intake%';
