package my.gov.jans.access.web;

import my.gov.jans.access.domain.*;
import my.gov.jans.access.repo.PenggunaRepository;
import my.gov.jans.access.repo.LokasRepository;
import my.gov.jans.access.repo.PermohonanRepository;
import my.gov.jans.access.repo.PenyeliaLojiRepository;
import my.gov.jans.access.service.AkaunService;
import my.gov.jans.access.service.PermohonanAccessCodeService;
import my.gov.jans.access.service.PermohonanService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class ApiController {
    private static final Logger LOG = LoggerFactory.getLogger(ApiController.class);
    private static final String VERIFIED_APPLICATION_NUMBER = "verifiedApplicationNumber";
    private static final String VERIFIED_APPLICATION_UNTIL = "verifiedApplicationUntil";
    private static final long VERIFIED_APPLICATION_TTL_MILLIS = 15 * 60 * 1000;

    private final PermohonanService s;
    private final PermohonanAccessCodeService accessCodeService;
    private final AkaunService akaunService;
    private final PenggunaRepository penggunaRepository;
    private final LokasRepository lokasRepository;
    private final PermohonanRepository permohonanRepository;
    private final PenyeliaLojiRepository penyeliaLojiRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom random = new SecureRandom();

    public ApiController(PermohonanService x, PermohonanAccessCodeService accessCodeService,
            AkaunService akaunService, PenggunaRepository penggunaRepository, LokasRepository lokasRepository,
            PermohonanRepository permohonanRepository,
            PenyeliaLojiRepository penyeliaLojiRepository, PasswordEncoder passwordEncoder) {
        s = x;
        this.accessCodeService = accessCodeService;
        this.akaunService = akaunService;
        this.penggunaRepository = penggunaRepository;
        this.lokasRepository = lokasRepository;
        this.permohonanRepository = permohonanRepository;
        this.penyeliaLojiRepository = penyeliaLojiRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/csrf")
    ResponseEntity<Map<String, String>> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(Map.of("token", csrfToken.getToken()));
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

    @PostMapping(value = "/public/permohonan", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> mohon(@Valid @RequestBody Permohonan p) {
        return ciptaPermohonan(p, null);
    }

    @PostMapping(value = "/public/permohonan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<?> mohonDenganDokumen(
            @Valid @RequestPart("permohonan") Permohonan p,
            @RequestPart("dokumenSokongan") MultipartFile dokumenSokongan) {
        return ciptaPermohonan(p, dokumenSokongan);
    }

    private ResponseEntity<?> ciptaPermohonan(Permohonan p, MultipartFile dokumenSokongan) {
        try {
            if (p.getLocationType() == null || p.getLocationName() == null
                    || p.getLocationName().isBlank()
                    || lokasRepository.findByType(p.getLocationType()).stream()
                            .noneMatch(lokasi -> p.getLocationName().trim().equals(lokasi.getName()))) {
                throw new IllegalArgumentException(
                        "Lokasi yang dipilih tidak lagi tersedia. Sila muat semula pilihan lokasi.");
            }
            if (dokumenSokongan == null || dokumenSokongan.isEmpty()) {
                throw new IllegalArgumentException("Dokumen sokongan diperlukan");
            }
            if (dokumenSokongan.getSize() > 5 * 1024 * 1024) {
                throw new IllegalArgumentException("Dokumen sokongan tidak boleh melebihi 5 MB");
            }

            String namaFail = Optional.ofNullable(dokumenSokongan.getOriginalFilename()).orElse("")
                    .replace('\\', '/');
            namaFail = namaFail.substring(namaFail.lastIndexOf('/') + 1).replaceAll("[\\r\\n\\t]", "_");
            if (!namaFail.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
                throw new IllegalArgumentException("Dokumen sokongan mestilah fail PDF");
            }
            if (namaFail.length() > 255) {
                namaFail = namaFail.substring(namaFail.length() - 255);
            }

            byte[] kandungan = dokumenSokongan.getBytes();
            if (kandungan.length < 5
                    || !"%PDF-".equals(new String(kandungan, 0, 5, StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("Kandungan dokumen sokongan bukan PDF yang sah");
            }

            p.setDokumenSokongan(kandungan);
            p.setDokumenSokonganNama(namaFail);
            return ResponseEntity.ok(Map.of("nomborPermohonan", s.cipta(p).getNomborPermohonan()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            s.hantarEmelKegagalanPermohonan(p.getEmailWakil(), e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            String mesejRamah = "Permohonan tidak dapat diproses buat masa ini. Sila semak semula maklumat yang diisi atau cuba beberapa saat lagi.";
            LOG.error("Gagal memproses permohonan baharu", e);
            s.hantarEmelKegagalanPermohonan(p.getEmailWakil(), mesejRamah);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("ralat", mesejRamah));
        }
    }

    @GetMapping({
            "/staff/permohonan/{id}/dokumen-sokongan",
            "/admin/permohonan/{id}/dokumen-sokongan",
            "/cgso/permohonan/{id}/dokumen-sokongan",
            "/kpp/permohonan/{id}/dokumen-sokongan"
    })
    ResponseEntity<?> muatDokumenSokongan(
            @PathVariable Long id,
            @RequestParam(name = "disposition", defaultValue = "attachment") String disposition) {
        Permohonan permohonan = permohonanRepository.findById(Objects.requireNonNull(id)).orElse(null);
        if (permohonan == null || permohonan.getDokumenSokongan() == null) {
            return ResponseEntity.notFound().build();
        }

        String namaFail = Optional.ofNullable(permohonan.getDokumenSokonganNama())
                .filter(nama -> !nama.isBlank())
                .orElse("dokumen-sokongan.pdf");
        String jenisPaparan = "inline".equalsIgnoreCase(disposition) ? "inline" : "attachment";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        jenisPaparan.equals("inline")
                                ? ContentDisposition.inline().filename(namaFail).build().toString()
                                : ContentDisposition.attachment().filename(namaFail).build().toString())
                .body(permohonan.getDokumenSokongan());
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

    @PostMapping("/public/permohonan/{no}/request-access-code")
    ResponseEntity<?> mintaKodSemakan(@PathVariable String no) {
        try {
            accessCodeService.mintaKod(no);
            return ResponseEntity.ok(Map.of(
                    "mesej",
                    "Jika nombor permohonan sah, kod pengesahan telah dihantar ke e-mel wakil berdaftar."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            LOG.error("Gagal memproses permintaan kod semakan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Permintaan kod tidak dapat diproses buat masa ini."));
        }
    }

    @PostMapping("/public/permohonan/{no}/verify-access-code")
    ResponseEntity<?> sahkanKodSemakan(
            @PathVariable String no,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        try {
            Permohonan permohonan = accessCodeService.sahkanKod(no, body.get("kod"));
            session.setAttribute(VERIFIED_APPLICATION_NUMBER, normalisasiNombor(permohonan.getNomborPermohonan()));
            session.setAttribute(
                    VERIFIED_APPLICATION_UNTIL,
                    System.currentTimeMillis() + VERIFIED_APPLICATION_TTL_MILLIS);
            return ResponseEntity.ok(paparanStatusAwam(permohonan));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", "Kod pengesahan tidak sah atau telah luput."));
        }
    }

    @GetMapping("/public/permohonan/{no}")
    ResponseEntity<?> semak(@PathVariable String no, Authentication auth, HttpSession session) {
        try {
            return ResponseEntity.ok(paparanStatusAwam(s.cari(no)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/public/permohonan/{no}/pdf")
    ResponseEntity<?> pdf(@PathVariable String no, Authentication auth, HttpSession session) {
        if (!bolehAksesPermohonan(no, auth, session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("ralat", "Pengesahan e-mel diperlukan sebelum memuat turun PDF."));
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=permohonan-" + no + ".pdf")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_PDF)).body(s.pdfPermohonan(s.cari(no)));
    }

    @GetMapping("/staff/permohonan")
    List<Permohonan> staf(@RequestParam(required = false) StatusPermohonan status) {
        return status == null ? s.senaraiSemua() : s.senarai(status);
    }

    @GetMapping("/staff/permohonan/{id}")
    Permohonan butiranStaf(@PathVariable String id) {
        return cariPermohonanUntukStaf(id);
    }

    @GetMapping("/staff/permohonan/{id}/pelawat")
    List<Pelawat> pelawatStaf(@PathVariable String id) {
        return cariPermohonanUntukStaf(id).getPelawat();
    }

    private Permohonan cariPermohonanUntukStaf(String rujukan) {
        String nilai = Optional.ofNullable(rujukan).orElse("").trim();
        if (nilai.matches("\\d+")) {
            return s.cari(Long.valueOf(nilai));
        }
        return s.cari(nilai);
    }

    @PostMapping("/staff/permohonan/{id}/hantar-pengarah")
    ResponseEntity<?> hantar(@PathVariable Long id, @RequestBody Map<String, String> b, Authentication a) {
        try {
            Permohonan permohonan = s.cari(id);
            if (permohonan.getKppNote() != null && !permohonan.getKppNote().isBlank()) {
                return ResponseEntity.ok(s.hantarPengarah(id, b.get("catatan"), a.getName()));
            }
            return ResponseEntity.ok(s.hantarCgso(id, b.get("catatan"), a.getName()));
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

    @PostMapping("/staff/permohonan/{id}/hantar-cgso")
    ResponseEntity<?> hantarCgso(@PathVariable Long id, @RequestBody Map<String, String> b, Authentication a) {
        try {
            return ResponseEntity.ok(s.hantarCgso(id, b.get("catatan"), a.getName()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ralat", "Permohonan atau pengguna tidak ditemui"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    @GetMapping("/cgso/permohonan")
    List<Permohonan> cgsoSenarai() {
        return s.senarai(StatusPermohonan.SEMAKAN_STAFF).stream()
                .filter(p -> p.getCgsoNote() == null || p.getCgsoNote().isBlank())
                .toList();
    }

    @GetMapping("/cgso/permohonan/{id}")
    Permohonan cgsoButiran(@PathVariable String id) {
        return cariPermohonanUntukStaf(id);
    }

    @GetMapping("/cgso/permohonan/{id}/pelawat")
    List<Pelawat> pelawatCgso(@PathVariable String id) {
        return cariPermohonanUntukStaf(id).getPelawat();
    }

    @PostMapping("/cgso/permohonan/{id}/hantar-kpp")
    ResponseEntity<?> cgsoHantarKpp(@PathVariable Long id, @RequestBody Map<String, String> b, Authentication a) {
        try {
            return ResponseEntity.ok(s.hantarKpp(id, b.get("catatan"), a.getName()));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ralat", "Permohonan tidak ditemui"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    @GetMapping("/cgso/permohonan/{id}/pdf")
    ResponseEntity<byte[]> cgsoPdf(@PathVariable Long id) {
        Permohonan p = s.cari(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=permohonan-" + p.getNomborPermohonan() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(s.pdfPermohonan(p));
    }

    @GetMapping("/kpp/permohonan")
    List<Permohonan> kppSenarai() {
        return s.senarai(StatusPermohonan.SEMAKAN_STAFF).stream()
                .filter(p -> p.getCgsoNote() != null && !p.getCgsoNote().isBlank())
                .filter(p -> p.getKppNote() == null || p.getKppNote().isBlank())
                .toList();
    }

    @GetMapping("/kpp/permohonan/{id}")
    Permohonan kppButiran(@PathVariable String id) {
        return cariPermohonanUntukStaf(id);
    }

    @GetMapping("/kpp/permohonan/{id}/pelawat")
    List<Pelawat> pelawatKpp(@PathVariable String id) {
        return cariPermohonanUntukStaf(id).getPelawat();
    }

    @PostMapping("/kpp/permohonan/{id}/hantar-petugas")
    ResponseEntity<?> kppHantarPetugas(@PathVariable Long id, @RequestBody Map<String, String> b, Authentication a) {
        try {
            return ResponseEntity.ok(s.hantarKembaliPetugas(id, b.get("catatan"), a.getName()));
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ralat", "Permohonan tidak ditemui"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    @GetMapping("/kpp/permohonan/{id}/pdf")
    ResponseEntity<byte[]> kppPdf(@PathVariable Long id) {
        Permohonan p = s.cari(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=permohonan-" + p.getNomborPermohonan() + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(s.pdfPermohonan(p));
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

    @GetMapping("/pengarah/permohonan/{id}")
    Permohonan butiranPengarah(@PathVariable String id) {
        return cariPermohonanUntukStaf(id);
    }

    @GetMapping("/pengarah/permohonan/{id}/pelawat")
    List<Pelawat> pelawatPengarah(@PathVariable String id) {
        return cariPermohonanUntukStaf(id).getPelawat();
    }

    @PostMapping("/pengarah/permohonan/{id}/keputusan")
    Permohonan putus(@PathVariable Long id, @RequestBody Map<String, Object> b, Authentication a) {
        return s.keputusan(id, Boolean.TRUE.equals(b.get("lulus")), (String) b.get("catatan"), a.getName());
    }

    @GetMapping("/penyelia/permohonan")
    List<Permohonan> penyeliaSenarai(Authentication a) {
        Pengguna user = penggunaRepository.findByEmail(a.getName()).orElseThrow();
        PenyeliaLoji penyelia = penyeliaLojiRepository.findByPengguna(user).orElse(null);
        List<String> daerahSeliaan = penyelia != null ? penyelia.getDaerahSeliaan() : List.of();
        if (daerahSeliaan.isEmpty()) {
            return List.of();
        }
        Set<String> namaLokasiDalamDaerah = lokasRepository.findAll().stream()
                .filter(l -> l.getDaerah() != null && daerahSeliaan.contains(l.getDaerah()))
                .map(lokasi -> lokasi.getName())
                .collect(Collectors.toSet());
        return s.senaraiSemua().stream()
                .filter(p -> namaLokasiDalamDaerah.contains(p.getLocationName()))
                .toList();
    }

    @PostMapping("/penyelia/permohonan/{id}/catatan")
    ResponseEntity<?> penyeliaCatatan(@PathVariable Long id, @RequestBody Map<String, String> b, Authentication a) {
        try {
            return ResponseEntity.ok(s.catatanPenyelia(id, b.get("catatan"), a.getName()));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("ralat", "Permohonan atau pengguna tidak ditemui"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat semasa menyimpan catatan penyelia loji"));
        }
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
        try {
            penggunaRepository.save(p);
            return ResponseEntity.ok(Map.of("berjaya", true, "enabled", p.isEnabled()));
        } catch (DataIntegrityViolationException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("ralat", "Status pengguna gagal dikemas kini."));
        }
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
        try {
            penggunaRepository.save(p);
            return ResponseEntity.ok(Map.of("berjaya", true));
        } catch (DataIntegrityViolationException e) {
            LOG.error("Gagal mengemas kini pengguna {}", p.getEmail(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("ralat",
                            "Pengguna gagal dikemas kini. Pastikan migrasi pangkalan data V6 telah dijalankan."));
        }
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
        try {
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
        } catch (DataIntegrityViolationException e) {
            LOG.error("Gagal mencipta pengguna {} kerana sekatan pangkalan data", email, e);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("ralat",
                            "Pengguna gagal dicipta. Pastikan migrasi pangkalan data V6 telah dijalankan dan peranan pengguna sah."));
        }
    }

    @DeleteMapping("/admin/users/{id}")
    @Transactional
    ResponseEntity<?> padamPenggunaAdmin(@PathVariable Long id) {
        if (!penggunaRepository.existsById(Objects.requireNonNull(id))) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }
        permohonanRepository.clearReviewedBy(id);
        permohonanRepository.clearDecidedBy(id);
        permohonanRepository.clearCompletedBy(id);
        penyeliaLojiRepository.deleteByPengguna_Id(id);
        penggunaRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("berjaya", true));
    }

    @GetMapping("/admin/penyelia/{userId}/daerah")
    ResponseEntity<?> senaraiDaerahSeliaan(@PathVariable Long userId) {
        Pengguna user = penggunaRepository.findById(Objects.requireNonNull(userId)).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }
        PenyeliaLoji penyelia = penyeliaLojiRepository.findByPengguna(user).orElse(null);
        return ResponseEntity.ok(Map.of("daerah", penyelia != null ? penyelia.getDaerahSeliaan() : List.of()));
    }

    @PostMapping("/admin/penyelia/{userId}/daerah")
    @Transactional
    ResponseEntity<?> tetapkanDaerahSeliaan(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        Pengguna user = penggunaRepository.findById(Objects.requireNonNull(userId)).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("ralat", "Pengguna tidak ditemui"));
        }
        if (user.getRole() != Role.PENYELIA_LOJI) {
            return ResponseEntity.badRequest().body(Map.of("ralat", "Pengguna bukan Penyelia Loji"));
        }
        Object daerahRaw = body.get("daerah");
        List<String> daerahList = new ArrayList<>();
        if (daerahRaw instanceof List<?> list) {
            for (Object item : list) {
                if (item != null && !item.toString().isBlank()) {
                    daerahList.add(item.toString().trim());
                }
            }
        }
        PenyeliaLoji penyelia = penyeliaLojiRepository.findByPengguna(user).orElseGet(() -> {
            PenyeliaLoji baru = new PenyeliaLoji();
            baru.setPengguna(user);
            return baru;
        });
        penyelia.setDaerahSeliaan(daerahList);
        penyeliaLojiRepository.save(penyelia);
        return ResponseEntity.ok(Map.of("berjaya", true, "daerah", penyelia.getDaerahSeliaan()));
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
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", p.getId());
        profile.put("name", p.getName());
        profile.put("email", p.getEmail());
        profile.put("phone", p.getPhone() != null ? p.getPhone() : "");
        profile.put("address", p.getAddress() != null ? p.getAddress() : "");
        profile.put("role", p.getRole().toString());
        if (p.getRole() == Role.PENYELIA_LOJI) {
            PenyeliaLoji penyelia = penyeliaLojiRepository.findByPengguna(p).orElse(null);
            List<String> daerah = penyelia != null ? penyelia.getDaerahSeliaan() : List.of();
            profile.put("daerah", daerah);
        }
        return ResponseEntity.ok(profile);
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

    private Map<String, Object> paparanStatusAwam(Permohonan permohonan) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("nomborPermohonan", permohonan.getNomborPermohonan());
        payload.put("status", permohonan.getStatus());
        payload.put("locationType", permohonan.getLocationType());
        payload.put("locationName", permohonan.getLocationName());
        if (permohonan.getStatus() == StatusPermohonan.PAS_DIKELUARKAN) {
            payload.put("passToken", permohonan.getPassToken());
        }
        return payload;
    }

    private boolean bolehAksesPermohonan(String no, Authentication auth, HttpSession session) {
        if (auth != null && auth.isAuthenticated()
                && auth.getAuthorities().stream()
                        .anyMatch(authority -> Set.of("ROLE_STAFF", "ROLE_PENGARAH", "ROLE_ADMIN")
                                .contains(authority.getAuthority()))) {
            return true;
        }

        Object nomborDisahkan = session.getAttribute(VERIFIED_APPLICATION_NUMBER);
        Object tempohSah = session.getAttribute(VERIFIED_APPLICATION_UNTIL);
        if (!(nomborDisahkan instanceof String) || !(tempohSah instanceof Long)
                || ((Long) tempohSah) <= System.currentTimeMillis()
                || !nomborDisahkan.equals(normalisasiNombor(no))) {
            return false;
        }
        return true;
    }

    private String normalisasiNombor(String no) {
        return Optional.ofNullable(no).orElse("").trim().toUpperCase(Locale.ROOT);
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
            item.put("daerah", l.getDaerah() != null ? l.getDaerah() : "");
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
                item.put("daerah", l.getDaerah() != null ? l.getDaerah() : "");
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
            String daerah = body.getOrDefault("daerah", "").trim();

            if (name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("ralat", "Nama lokasi tidak boleh kosong"));
            }

            JenisLokasi jenisLokasi;
            try {
                jenisLokasi = JenisLokasi.valueOf(typeStr);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest()
                        .body(Map.of("ralat", "Jenis lokasi tidak sah. Pilih Loji Rawatan Air atau Muka Sauk"));
            }

            Lokasi lokasi = new Lokasi();
            lokasi.setType(jenisLokasi);
            lokasi.setName(name);
            lokasi.setDaerah(daerah);
            lokasRepository.save(lokasi);

            return ResponseEntity.ok(Map.of(
                    "berjaya", true,
                    "lokasi", Map.of(
                            "id", lokasi.getId(),
                            "type", lokasi.getType().name(),
                            "name", lokasi.getName(),
                            "daerah", lokasi.getDaerah() != null ? lokasi.getDaerah() : "")));
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
            if (body.containsKey("daerah") && body.get("daerah") != null) {
                lokasi.setDaerah(body.get("daerah"));
            }
            lokasRepository.save(lokasi);
            return ResponseEntity.ok(Map.of("berjaya", true));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("ralat", "Ralat mengemaskini lokasi: " + e.getMessage()));
        }
    }

    @DeleteMapping("/locations/{id}")
    public ResponseEntity<?> padamLokasi(@PathVariable Long id) {
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