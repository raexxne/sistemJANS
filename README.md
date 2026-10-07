# Sistem Kebenaran Masuk JAS

Aplikasi Spring Boot + MySQL untuk mengurus permohonan akses loji dan intake.

## Menjalankan aplikasi secara setempat

1. Cipta pangkalan data menggunakan `database/jans_access.sql`.
2. Salin `src/main/resources/application.example.properties` sebagai `src/main/resources/application.properties`.
3. Tetapkan pemboleh ubah persekitaran yang diperlukan sebelum menjalankan aplikasi:

   ```powershell
   $env:DB_URL = "jdbc:mysql://localhost:3306/jans_access?serverTimezone=Asia/Kuala_Lumpur"
   $env:DB_USERNAME = "root"
   $env:DB_PASSWORD = "<kata laluan pangkalan data>"
   $env:APP_PUBLIC_URL = "http://localhost:8080"
   ```

   Jika pangkalan data belum mempunyai akaun `ADMIN`, tetapkan juga `APP_BOOTSTRAP_ADMIN_EMAIL` dan `APP_BOOTSTRAP_ADMIN_PASSWORD`. Kata laluan bootstrap mesti sekurang-kurangnya 16 aksara. Sistem tidak lagi mencipta akaun admin dengan kata laluan lalai.

4. Jalankan `mvn spring-boot:run`.
5. Buka `http://localhost:8080`.

Dump SQL mengandungi akaun demo untuk pembangunan setempat sahaja. Tukar kata laluan semua akaun demo dan buang data contoh sebelum sistem digunakan di luar persekitaran pembangunan.

Kredensial demo selepas import SQL (pembangunan setempat sahaja):

- Kakitangan: `staff@jans.gov.my` / `Staff123!`
- Pengarah: `pengarah@jans.gov.my` / `Pengarah123!`
- Admin: `admin@jans.gov.my` / `Admin123!`

## Konfigurasi

Konfigurasi sensitif dibaca daripada environment, bukan disimpan dalam repositori:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`
- `APP_PUBLIC_URL`
- `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD` (hanya diperlukan jika tiada admin)
- `APP_COOKIE_SECURE` (`true` apabila aplikasi dihidangkan melalui HTTPS)

Flyway menjalankan migrasi pangkalan data daripada `src/main/resources/db/migration`. Oleh sebab skema sedia ada diwujudkan melalui SQL dump, migrasi membuat baseline versi `0` secara automatik sebelum migrasi baharu.

Token reset kata laluan disimpan sebagai hash dalam pangkalan data, luput selepas 10 minit, dan dihadkan kepada lima percubaan kod serta satu permintaan baharu seminit. Token CSRF digunakan untuk permintaan yang mengubah data, dan halaman web menghantar token itu secara automatik. Health check Actuator tersedia di `/actuator/health`; butiran dalaman health check tidak didedahkan.

Jalankan ujian dengan `mvn test`.
