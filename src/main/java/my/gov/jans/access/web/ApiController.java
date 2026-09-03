package my.gov.jans.access.web;

import my.gov.jans.access.domain.*;
import my.gov.jans.access.repo.PenggunaRepository;
import my.gov.jans.access.repo.LokasRepository;
import my.gov.jans.access.service.AkaunService;
import my.gov.jans.access.service.PermohonanService;
import org.springframework.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.security.SecureRandom;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final PermohonanService s;
    private final AkaunService akaunService;
    private final PenggunaRepository penggunaRepository;
    private final LokasRepository lokasRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public ApiController(PermohonanService x, AkaunService akaunService, PenggunaRepository penggunaRepository,
            LokasRepository lokasRepository, PasswordEncoder passwordEncoder) {
        s = x;
        this.akaunService = akaunService;
        this.penggunaRepository = penggunaRepository;
        this.lokasRepository = lokasRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/public/forgot-password")
    ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body) {
        try {
            akaunService.hantarKodReset(body.get("email"));
            return ResponseEntity.ok(Map.of("mesej", "Jika e-mel didaftarkan, kod 6 digit telah dihantar."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat semasa reset kata laluan"));
        }
    }

    @PostMapping("/public/forgot-password/request-code")
    ResponseEntity<?> requestKodReset(@RequestBody Map<String, String> body) {
        return forgotPassword(body);
    }

    @PostMapping("/public/forgot-password/verify-code")
    ResponseEntity<?> verifyKodReset(@RequestBody Map<String, String> body) {
        try {
            akaunService.sahkanKodDanResetKataLaluan(body.get("email"), body.get("kod"));
            return ResponseEntity.ok(Map.of("mesej", "Kod disahkan. Kata laluan baharu telah dihantar ke e-mel anda."));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat semasa pengesahan kod reset"));
        }
    }

    @PostMapping("/public/permohonan")
    ResponseEntity<?> mohon(@RequestBody Permohonan p) {
        try {
            return ResponseEntity.ok(Map.of("nomborPermohonan", s.cipta(p).getNomborPermohonan()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            s.hantarEmelKegagalanPermohonan(p.getEmailWakil(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            String mesejRamah = "Permohonan tidak dapat diproses buat masa ini. Sila semak semula maklumat yang diisi atau cuba beberapa saat lagi.";
            s.hantarEmelKegagalanPermohonan(p.getEmailWakil(), mesejRamah);
            return ResponseEntity.badRequest().body(Map.of("ralat", mesejRamah));
        }
    }

    @PostMapping("/public/hubungi")
    ResponseEntity<?> hubungi(@RequestBody Map<String, String> body) {
        String nama = Objects.requireNonNull(body.get("nama"), "Nama diperlukan");
        String email = Objects.requireNonNull(body.get("email"), "E-mel diperlukan");
        String subjek = body.getOrDefault("subjek", "pertanyaan-umum");
        String mesej = Objects.requireNonNull(body.get("mesej"), "Mesej diperlukan");
        String isi = "<p>Mesej daripada: <b>" + nama + "</b> (" + email + ")</p>"
                + "<p>Subjek: " + subjek + "</p>"
                + "<p>" + mesej.replace("\n", "<br>") + "</p>";
        s.hantarEmailHubungi(email, nama, subjek, isi);
        return ResponseEntity.ok(Map.of("berjaya", true));
    }

    @PostMapping("/public/aduan")
    ResponseEntity<?> aduan(@RequestBody Map<String, String> body) {
        String rujukan = "ADU-" + System.currentTimeMillis();
        String nama = Objects.requireNonNull(body.get("nama"), "Nama diperlukan");
        String email = Objects.requireNonNull(body.get("email"), "E-mel diperlukan");
        String jenisAduan = body.getOrDefault("jenisAduan", "-");
        String butiran = Objects.requireNonNull(body.get("butiran"), "Butiran diperlukan");
        String tindakan = body.getOrDefault("tindakan", "-");
        String noPermohonan = body.getOrDefault("noPermohonan", "-");
        String isi = "<p>Aduan baharu diterima.</p>"
                + "<p><b>Rujukan:</b> " + rujukan + "</p>"
                + "<p><b>Nama:</b> " + nama + "</p>"
                + "<p><b>E-mel:</b> " + email + "</p>"
                + "<p><b>No. Permohonan:</b> " + noPermohonan + "</p>"
                + "<p><b>Jenis Aduan:</b> " + jenisAduan + "</p>"
                + "<p><b>Butiran:</b><br>" + butiran.replace("\n", "<br>") + "</p>"
                + "<p><b>Tindakan Diminta:</b><br>" + tindakan.replace("\n", "<br>") + "</p>";
        s.hantarEmailAduan(email, nama, rujukan, isi);
        return ResponseEntity.ok(Map.of("rujukan", rujukan, "berjaya", true));
    }

    @GetMapping("/public/permohonan/{no}")
    Permohonan semak(@PathVariable String no) {
        return s.cari(no);
    }

    @GetMapping("/public/permohonan/{no}/pdf")
    ResponseEntity<byte[]> pdf(@PathVariable String no) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=permohonan-" + no + ".pdf")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF)).body(s.pdfPermohonan(s.cari(no)));
    }

    @GetMapping("/staff/permohonan")
    List<Permohonan> staf(@RequestParam(required = false) StatusPermohonan status) {
        return status == null ? s.senaraiSemua() : s.senarai(status);
    }

    @GetMapping("/staff/permohonan/{id}")
    Permohonan butiranStaf(@PathVariable Long id) {
        return s.cari(id);
    }

    @PostMapping("/staff/permohonan/{id}/hantar-pengarah")
    ResponseEntity<?> hantar(@PathVariable Long id, @RequestBody Map<String, String> b, Authentication a) {
        try {
            return ResponseEntity.ok(s.hantarPengarah(id, b.get("catatan"), a.getName()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ralat", "Permohonan atau pengguna tidak ditemui"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat semasa menghantar kepada pengarah"));
        }
    }

    @DeleteMapping("/staff/permohonan/{id}")
    ResponseEntity<?> padamStaf(@PathVariable Long id) {
        try {
            s.padamPermohonanStaf(id);
            return ResponseEntity.ok(Map.of("berjaya", true));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    @GetMapping("/pengarah/permohonan")
    List<Permohonan> pengarah() {
        return s.senarai(StatusPermohonan.MENUNGGU_PENGARAH);
    }

    @PostMapping("/pengarah/permohonan/{id}/keputusan")
    Permohonan putus(@PathVariable Long id, @RequestBody Map<String, Object> b, Authentication a) {
        return s.keputusan(id, Boolean.TRUE.equals(b.get("lulus")), (String) b.get("catatan"), a.getName());
    }

    @GetMapping("/pas/{token}")
    Permohonan sah(@PathVariable String token) {
        return s.cariToken(token);
    }

    @GetMapping("/pas/{token}/pdf")
    ResponseEntity<byte[]> muatPas(@PathVariable String token) {
        Permohonan p = s.cariToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=pas-" + p.getNomborPermohonan() + ".pdf")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF)).body(s.pdfPas(p));
    }

    @GetMapping("/pas/{token}/pdf-tanpa-qr")
    ResponseEntity<byte[]> muatPasTanpaQR(@PathVariable String token) {
        Permohonan p = s.cariToken(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=pas-" + p.getNomborPermohonan() + ".pdf")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF)).body(s.pdfPasTanpaQR(p));
    }

    @GetMapping("/pas/{token}/qr")
    ResponseEntity<byte[]> muatQR(@PathVariable String token) {
        Permohonan p = s.cariToken(token);
        return ResponseEntity.ok().contentType(Objects.requireNonNull(MediaType.IMAGE_PNG)).body(s.qrCodeImage(p));
    }

    @GetMapping("/admin/users")
    ResponseEntity<?> senaraiPenggunaAdmin() {
        List<Pengguna> pengguna = penggunaRepository.findAll();
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Pengguna p : pengguna) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", p.getId());
            item.put("name", p.getName());
            item.put("email", p.getEmail());
            item.put("role", p.getRole().name());
            item.put("enabled", p.isEnabled());
            item.put("phone", p.getPhone() != null ? p.getPhone() : "");
            item.put("address", p.getAddress() != null ? p.getAddress() : "");
            payload.add(item);
        }
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/admin/users/{id}")
    ResponseEntity<?> butiranPenggunaAdmin(@PathVariable Long id) {
        return penggunaRepository.findById(Objects.requireNonNull(id))
                .map(p -> ResponseEntity.ok(Map.of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "email", p.getEmail(),
                        "role", p.getRole().name(),
                        "enabled", p.isEnabled(),
                        "phone", p.getPhone() != null ? p.getPhone() : "",
                        "address", p.getAddress() != null ? p.getAddress() : "")))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("ralat", "Pengguna tidak ditemui")));
    }

    @GetMapping("/admin/users/{id}/picture")
    ResponseEntity<byte[]> getUserPictureAdmin(@PathVariable Long id) {
        Pengguna p = penggunaRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (p == null || p.getProfilePicture() == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = p.getProfilePictureType() != null ? p.getProfilePictureType() : "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Objects.requireNonNull(contentType)))
                .body(p.getProfilePicture());
    }

    @PostMapping("/admin/users/{id}/status")
    ResponseEntity<?> tukarStatusPengguna(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Pengguna p = penggunaRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }
        Boolean enabled = (Boolean) body.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("ralat", "Status diperlukan"));
        }
        p.setEnabled(enabled);
        penggunaRepository.save(p);
        return ResponseEntity.ok(Map.of("berjaya", true, "enabled", p.isEnabled()));
    }

    @PutMapping("/admin/users/{id}")
    ResponseEntity<?> kemasKiniPenggunaAdmin(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Pengguna p = penggunaRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }
        if (body.containsKey("name") && body.get("name") != null) {
            p.setName(body.get("name"));
        }
        if (body.containsKey("email") && body.get("email") != null) {
            p.setEmail(body.get("email"));
        }
        if (body.containsKey("phone") && body.get("phone") != null) {
            p.setPhone(body.get("phone"));
        }
        if (body.containsKey("address") && body.get("address") != null) {
            p.setAddress(body.get("address"));
        }
        if (body.containsKey("role") && body.get("role") != null) {
            try {
                p.setRole(Role.valueOf(body.get("role")));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("ralat", "Peranan tidak sah"));
            }
        }
        penggunaRepository.save(p);
        return ResponseEntity.ok(Map.of("berjaya", true));
    }

    @PostMapping("/admin/users")
    ResponseEntity<?> ciptaPenggunaAdmin(@RequestBody Map<String, String> body) {
        String email = normalisasiEmail(body.get("email"));
        if (penggunaRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("ralat", "E-mel sudah wujud"));
        }

        String password = Optional.ofNullable(body.get("password"))
                .filter(p -> !p.isBlank())
                .orElseGet(() -> janaKataLaluan(10));

        String roleRaw = Optional.ofNullable(body.get("role")).orElse("STAFF").trim().toUpperCase();
        Role role;
        try {
            role = Role.valueOf(roleRaw);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", "Peranan tidak sah"));
        }

        Pengguna pengguna = new Pengguna();
        pengguna.setName(Optional.ofNullable(body.get("name")).orElse(""));
        pengguna.setEmail(email);
        pengguna.setPasswordHash(passwordEncoder.encode(password));
        pengguna.setRole(role);
        pengguna.setEnabled(Boolean.parseBoolean(Optional.ofNullable(body.get("enabled")).orElse("true")));
        pengguna.setPhone(Optional.ofNullable(body.get("phone")).orElse(""));
        pengguna.setAddress(Optional.ofNullable(body.get("address")).orElse(""));
        penggunaRepository.save(pengguna);

        return ResponseEntity.ok(Map.of(
                "berjaya", true,
                "password", password,
                "user", Map.of(
                        "id", pengguna.getId(),
                        "name", pengguna.getName(),
                        "email", pengguna.getEmail(),
                        "role", pengguna.getRole().name(),
                        "enabled", pengguna.isEnabled())));
    }

    @DeleteMapping("/admin/users/{id}")
    ResponseEntity<?> padamPenggunaAdmin(@PathVariable Long id) {
        if (!penggunaRepository.existsById(Objects.requireNonNull(id))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }
        penggunaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("berjaya", true));
    }

    @GetMapping("/profile")
    ResponseEntity<?> getProfile(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ralat", "Tidak dibenarkan"));
        }
        Pengguna p = penggunaRepository.findByEmail(auth.getName()).orElse(null);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }
        return ResponseEntity.ok(Map.of(
                "id", p.getId(),
                "name", p.getName(),
                "email", p.getEmail(),
                "phone", p.getPhone() != null ? p.getPhone() : "",
                "address", p.getAddress() != null ? p.getAddress() : "",
                "role", p.getRole().toString()));
    }

    private String normalisasiEmail(String email) {
        String emel = Optional.ofNullable(email).orElse("").trim().toLowerCase();
        if (emel.isEmpty()) {
            throw new IllegalArgumentException("E-mel diperlukan");
        }
        return emel;
    }

    private String janaKataLaluan(int panjang) {
        String chars = "abcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder(panjang);
        for (int i = 0; i < panjang; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    @PostMapping("/profile/update")
    ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ralat", "Tidak dibenarkan"));
        }
        Pengguna p = penggunaRepository.findByEmail(auth.getName()).orElse(null);
        if (p == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }

        if (body.containsKey("name") && body.get("name") != null) {
            p.setName(body.get("name"));
        }
        if (body.containsKey("email") && body.get("email") != null) {
            String emailBaru = normalisasiEmail(body.get("email"));
            if (!emailBaru.equalsIgnoreCase(p.getEmail())
                    && penggunaRepository.findByEmail(emailBaru).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("ralat", "E-mel sudah wujud"));
            }
            p.setEmail(emailBaru);
            Authentication updatedAuth = new UsernamePasswordAuthenticationToken(
                    emailBaru,
                    auth.getCredentials(),
                    auth.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(updatedAuth);
        }
        if (body.containsKey("phone") && body.get("phone") != null) {
            p.setPhone(body.get("phone"));
        }
        if (body.containsKey("address") && body.get("address") != null) {
            p.setAddress(body.get("address"));
        }

        penggunaRepository.save(p);
        return ResponseEntity.ok(Map.of("berjaya", true, "mesej", "Profil berjaya dikemaskini"));
    }

    @PostMapping("/profile/change-password")
    ResponseEntity<?> tukarKataLaluan(@RequestBody Map<String, String> body, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ralat", "Tidak dibenarkan"));
        }

        try {
            akaunService.tukarKataLaluan(
                    auth.getName(),
                    body.get("newPassword"),
                    body.get("confirmPassword"));
            return ResponseEntity.ok(Map.of("berjaya", true, "mesej", "Kata laluan berjaya dikemaskini"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", e.getMessage()));
        }
    }

    @PostMapping("/profile/upload-picture")
    ResponseEntity<?> uploadPicture(@RequestParam("picture") MultipartFile file, Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("ralat", "Tidak dibenarkan"));
        }
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("ralat", "Fail kosong"));
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            return ResponseEntity.badRequest().body(Map.of("ralat", "Saiz fail melebihi 5MB"));
        }

        try {
            Pengguna p = penggunaRepository.findByEmail(auth.getName()).orElse(null);
            if (p == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
            }

            p.setProfilePicture(file.getBytes());
            p.setProfilePictureType(file.getContentType());
            penggunaRepository.save(p);

            return ResponseEntity.ok(Map.of("berjaya", true, "mesej", "Gambar profil berjaya dimuat naik"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat memuat naik gambar: " + e.getMessage()));
        }
    }

    @GetMapping("/profile/picture")
    ResponseEntity<byte[]> getPicture(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Pengguna p = penggunaRepository.findByEmail(auth.getName()).orElse(null);
        if (p == null || p.getProfilePicture() == null) {
            return ResponseEntity.notFound().build();
        }

        String contentType = p.getProfilePictureType() != null ? p.getProfilePictureType() : "image/jpeg";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(Objects.requireNonNull(contentType)))
                .body(p.getProfilePicture());
    }

    // ===== LOKASI MANAGEMENT ENDPOINTS =====

    @GetMapping("/public/locations")
    ResponseEntity<?> senaraiLokasi() {
        List<Lokasi> lokasi = lokasRepository.findAll();
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Lokasi l : lokasi) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", l.getId());
            item.put("type", l.getType().name());
            item.put("name", l.getName());
            item.put("address", l.getAddress() != null ? l.getAddress() : "");
            payload.add(item);
        }
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/public/locations/by-type/{type}")
    ResponseEntity<?> lokasiBerdasarkanJenis(@PathVariable String type) {
        try {
            JenisLokasi jenisLokasi = JenisLokasi.valueOf(type.toUpperCase());
            List<Lokasi> lokasi = lokasRepository.findByType(jenisLokasi);
            List<Map<String, Object>> payload = new ArrayList<>();
            for (Lokasi l : lokasi) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", l.getId());
                item.put("type", l.getType().name());
                item.put("name", l.getName());
                item.put("address", l.getAddress() != null ? l.getAddress() : "");
                payload.add(item);
            }
            return ResponseEntity.ok(payload);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", "Jenis lokasi tidak sah"));
        }
    }

    @PostMapping("/locations")
    ResponseEntity<?> ciptaLokasi(@RequestBody Map<String, String> body) {
        try {
            String typeStr = Objects.requireNonNull(body.get("type"), "Jenis lokasi diperlukan").trim().toUpperCase();
            String name = Objects.requireNonNull(body.get("name"), "Nama lokasi diperlukan").trim();
            String address = body.getOrDefault("address", "").trim();

            if (name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ralat", "Nama lokasi tidak boleh kosong"));
            }

            JenisLokasi jenisLokasi;
            try {
                jenisLokasi = JenisLokasi.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ralat", "Jenis lokasi tidak sah. Pilih LOJI atau INTAKE"));
            }

            Lokasi lokasi = new Lokasi();
            lokasi.setType(jenisLokasi);
            lokasi.setName(name);
            lokasi.setAddress(address);
            lokasRepository.save(lokasi);

            return ResponseEntity.ok(Map.of(
                    "berjaya", true,
                    "lokasi", Map.of(
                            "id", lokasi.getId(),
                            "type", lokasi.getType().name(),
                            "name", lokasi.getName(),
                            "address", lokasi.getAddress() != null ? lokasi.getAddress() : "")));
        } catch (NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat mencipta lokasi: " + e.getMessage()));
        }
    }

    @PutMapping("/locations/{id}")
    ResponseEntity<?> kemasKiniLokasi(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Lokasi lokasi = lokasRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (lokasi == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Lokasi tidak ditemui"));
        }

        try {
            if (body.containsKey("type") && body.get("type") != null) {
                try {
                    lokasi.setType(JenisLokasi.valueOf(body.get("type").toUpperCase()));
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("ralat", "Jenis lokasi tidak sah"));
                }
            }
            if (body.containsKey("name") && body.get("name") != null) {
                String name = body.get("name").trim();
                if (name.isEmpty()) {
                    return ResponseEntity.badRequest().body(Map.of("ralat", "Nama lokasi tidak boleh kosong"));
                }
                lokasi.setName(name);
            }
            if (body.containsKey("address") && body.get("address") != null) {
                lokasi.setAddress(body.get("address"));
            }
            lokasRepository.save(lokasi);
            return ResponseEntity.ok(Map.of("berjaya", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat mengemaskini lokasi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/locations/{id}")
    ResponseEntity<?> padamLokasi(@PathVariable Long id) {
        if (!lokasRepository.existsById(Objects.requireNonNull(id))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Lokasi tidak ditemui"));
        }
        try {
            lokasRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("berjaya", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat memadam lokasi: " + e.getMessage()));
        }
    }
}