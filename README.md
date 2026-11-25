# Sistem Kebenaran Masuk JAS

Aplikasi Spring Boot + MySQL untuk mengurus permohonan akses loji dan muka sauk.

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

## Membina fail WAR

Jalankan arahan berikut dari folder projek:

```powershell
mvn clean package
```

Fail WAR akan dijana di `target/jans-access-system.war`. Fail ini boleh digunakan pada servlet container yang serasi dengan Jakarta Servlet 6.0 dan Java 25 atau lebih baharu. Konfigurasi pangkalan data dan pemboleh ubah persekitaran yang diterangkan di atas masih diperlukan apabila aplikasi dijalankan.

Dump SQL mengandungi akaun demo untuk pembangunan setempat sahaja. Tukar kata laluan semua akaun demo dan buang data contoh sebelum sistem digunakan di luar persekitaran pembangunan.

Kredensial demo selepas import SQL (pembangunan setempat sahaja):

- Kakitangan: `staff@jans.gov.my` / `Staff123!`
- Pengarah: `pengarah@jans.gov.my` / `Pengarah123!`
- Admin: `admin@jans.gov.my` / `Admin123!`
- Penyelia Loji: `penyelia@jans.gov.my` / `Penyelia123!`

## Konfigurasi

Konfigurasi sensitif dibaca daripada environment, bukan disimpan dalam repositori:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`
- `APP_PUBLIC_URL`
- `APP_BOOTSTRAP_ADMIN_EMAIL`, `APP_BOOTSTRAP_ADMIN_PASSWORD` (hanya diperlukan jika tiada admin)
- `APP_COOKIE_SECURE` (`true` apabila aplikasi dihidangkan melalui HTTPS)

Untuk notifikasi pengarah, tetapkan `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` dan `SMTP_PASSWORD` pada proses aplikasi. Untuk Gmail, gunakan App Password, bukan kata laluan akaun biasa. Pastikan akaun berperanan `PENGARAH` mempunyai alamat e-mel sebenar dan `APP_PUBLIC_URL` boleh dicapai oleh pengarah (termasuk nama aplikasi jika digunakan sebagai WAR). Apabila petugas menekan **Hantar ke Pengarah**, sistem menghantar notifikasi kepada alamat e-mel pengarah yang berdaftar. Jika penghantaran gagal, petugas akan menerima mesej ralat dan boleh mencuba semula selepas konfigurasi SMTP diperbetulkan. Mod `app.mail.mock=true` hanya merekodkan e-mel dan tidak menghantarnya.

Notifikasi aliran permohonan dihantar kepada semua akaun peranan yang berkenaan: CGSO apabila petugas menghantar semakan, KPP apabila CGSO menghantar semakan, dan petugas apabila KPP memberi maklum balas atau penyelia menandakan lawatan selesai. Keputusan Pengarah dihantar kepada pemohon dan semua petugas. Pastikan setiap peranan mempunyai akaun dengan alamat e-mel yang sah dan template notifikasi berkaitan aktif dalam pengaturan admin. Jika e-mel wajib gagal dihantar, tindakan dibatalkan dan pengguna dimaklumkan. E-mel yang sudah dihantar kepada penerima lain tidak boleh ditarik balik; percubaan semula boleh menyebabkan e-mel pendua.

Dalam pengaturan admin, kunci pengumuman boleh dipilih sendiri tetapi mesti unik. Kod status sedia ada tidak boleh diubah. Lencana, kad dan carta dashboard petugas serta penyelia menggunakan warna status yang ditetapkan admin. Kad dan carta semakan yang belum selesai menggabungkan `DIHANTAR` dan `SEMAKAN_STAFF` menggunakan warna `DIHANTAR` (kelabu secara lalai); permohonan yang sudah menerima ulasan KPP menggunakan warna semakan selesai (biru secara lalai). Kad dan carta petugas yang menggabungkan `DILULUSKAN` dengan `PAS_DIKELUARKAN` menggunakan warna `DILULUSKAN`; kad jumlah keseluruhan tidak bergantung pada status. Teks lencana berwarna putih, jadi pilih latar yang cukup gelap untuk dibaca. Dashboard pada peranti lain perlu dimuat semula untuk mengambil tetapan terkini.

Flyway menjalankan migrasi pangkalan data daripada `src/main/resources/db/migration`. Oleh sebab skema sedia ada diwujudkan melalui SQL dump, migrasi membuat baseline versi `0` secara automatik sebelum migrasi baharu.

Token reset kata laluan dan kod akses semakan permohonan disimpan sebagai hash dalam pangkalan data, luput selepas 10 minit, dan dihadkan kepada lima percubaan serta satu permintaan baharu seminit. Status terperinci, token pas dan PDF permohonan hanya boleh dicapai selepas kod dihantar ke e-mel wakil disahkan; sesi pengesahan sah selama 15 minit. Token CSRF digunakan untuk permintaan yang mengubah data, dan halaman web menghantar token itu secara automatik. Health check Actuator tersedia di `/actuator/health`; butiran dalaman health check tidak didedahkan.

Jalankan ujian dengan `mvn test`.
