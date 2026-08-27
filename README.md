# Sistem Kebenaran Akses JANS

Aplikasi Spring Boot + MySQL untuk mengurus permohonan akses loji dan intake.

## Menjalankan aplikasi

1. Cipta pangkalan data menggunakan `database/jans_access.sql`.
2. Salin `src/main/resources/application.example.properties` sebagai `application.properties` dan isi kata laluan MySQL serta SMTP.
3. Jalankan `mvn spring-boot:run`.
4. Buka `http://localhost:8080`.

Pengguna contoh selepas menjalankan SQL:

- Kakitangan: `staff@jans.gov.my` / `Staff123!`
- Pengarah: `pengarah@jans.gov.my` / `Pengarah123!`
- Admin: `admin@jans.gov.my` / `Admin123!`

Untuk kegunaan produksi, tukar kata laluan contoh, tetapkan `app.public-url` kepada domain sebenar dan gunakan SMTP rasmi JANS.
