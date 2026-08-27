package my.gov.jans.access.service;

import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.repo.PenggunaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AkaunService {

    private static final String AKSARA = "abcdefghjkmnpqrstuvwxyz23456789";

    private final PenggunaRepository penggunaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, ResetKodInfo> resetKodStore = new ConcurrentHashMap<>();
    private final Map<String, Deque<String>> sejarahKataLaluanStore = new ConcurrentHashMap<>();

    private static final class ResetKodInfo {
        private final String kod;
        private final Instant tamatPada;

        private ResetKodInfo(String kod, Instant tamatPada) {
            this.kod = kod;
            this.tamatPada = tamatPada;
        }
    }

    public AkaunService(PenggunaRepository penggunaRepository, PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.penggunaRepository = penggunaRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional(readOnly = true)
    public void hantarKodReset(String email) {
        String emel = normalisasiEmail(email);
        Pengguna pengguna = penggunaRepository.findByEmail(emel).orElse(null);
        if (pengguna == null) {
            return;
        }

        String kod = String.format("%06d", random.nextInt(1_000_000));
        resetKodStore.put(emel, new ResetKodInfo(kod, Instant.now().plusSeconds(600)));

        String isi = "<p>Tuan/Puan,</p>"
                + "<p>Kod 6 digit untuk reset kata laluan akaun JANS anda ialah:</p>"
                + "<p style=\"font-size: 1.4rem; font-weight: 700; letter-spacing: 0.12em;\">" + kod + "</p>"
                + "<p>Kod ini sah selama <b>10 minit</b>.</p>"
                + "<p>Jika anda tidak meminta reset kata laluan, sila abaikan e-mel ini.</p>";

        emailService.hantarEmail(
                pengguna.getEmail(),
                "Kod Reset Kata Laluan JANS",
                isi,
                null,
                null);
    }

    @Transactional
    public void sahkanKodDanResetKataLaluan(String email, String kod) {
        String emel = normalisasiEmail(email);
        String kodBersih = Objects.requireNonNull(kod, "Kod reset diperlukan").trim();
        if (!kodBersih.matches("\\d{6}")) {
            throw new IllegalArgumentException("Kod reset mesti 6 digit");
        }

        ResetKodInfo info = resetKodStore.get(emel);
        if (info == null || Instant.now().isAfter(info.tamatPada) || !info.kod.equals(kodBersih)) {
            resetKodStore.remove(emel);
            throw new IllegalStateException("Kod reset tidak sah atau telah tamat");
        }

        Pengguna pengguna = penggunaRepository.findByEmail(emel)
                .orElseThrow(() -> new IllegalStateException("Akaun tidak ditemui"));

        String kataLaluanBaharu = janaKataLaluan(10);
        pengguna.setPasswordHash(passwordEncoder.encode(kataLaluanBaharu));
        penggunaRepository.save(pengguna);
        resetKodStore.remove(emel);

        String isi = "<p>Tuan/Puan,</p>"
                + "<p>Kata laluan baharu untuk akaun JANS anda telah dijana.</p>"
                + "<p><b>Kata laluan baharu:</b></p>"
                + "<p style=\"font-size: 1.2rem; font-weight: 700; letter-spacing: 0.08em;\">" + kataLaluanBaharu
                + "</p>"
                + "<p>Sila log masuk semula menggunakan kata laluan ini.</p>"
                + "<p><small>Tip: Kata laluan ini hanya mengandungi huruf kecil dan nombor.</small></p>"
                + "<p>Terima kasih.</p>";

        emailService.hantarEmail(
                pengguna.getEmail(),
                "Reset Kata Laluan JANS",
                isi,
                null,
                null);
    }

    @Transactional
    public void tukarKataLaluan(String email, String kataLaluanBaharu, String pengesahanKataLaluan) {
        String emel = normalisasiEmail(email);
        Pengguna pengguna = penggunaRepository.findByEmail(emel)
                .orElseThrow(() -> new IllegalStateException("Akaun tidak ditemui"));

        String kataLaluan = Optional.ofNullable(kataLaluanBaharu).orElse("").trim();
        String pengesahan = Optional.ofNullable(pengesahanKataLaluan).orElse("").trim();

        if (kataLaluan.isEmpty()) {
            throw new IllegalArgumentException("Kata laluan baharu diperlukan");
        }
        if (pengesahan.isEmpty()) {
            throw new IllegalArgumentException("Sahkan kata laluan diperlukan");
        }
        if (!kataLaluan.equals(pengesahan)) {
            throw new IllegalArgumentException("Kata laluan baharu dan pengesahan kata laluan tidak sepadan");
        }
        if (kataLaluan.length() < 8 || kataLaluan.length() > 12) {
            throw new IllegalArgumentException("Kata laluan mesti mempunyai 8 hingga 12 aksara");
        }
        if (!kataLaluan.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$")) {
            throw new IllegalArgumentException(
                    "Kata laluan mesti mengandungi huruf besar, huruf kecil, nombor dan simbol");
        }
        if (mengandungiTigaAksaraBerturutan(kataLaluan)) {
            throw new IllegalArgumentException(
                    "Kata laluan tidak boleh mengandungi urutan 3 aksara berturutan seperti 123 atau abc");
        }

        String namaBersih = Optional.ofNullable(pengguna.getName()).orElse("")
                .replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase();
        if (!namaBersih.isEmpty() && kataLaluan.toLowerCase().contains(namaBersih)) {
            throw new IllegalArgumentException("Kata laluan tidak boleh menyerupai nama anda");
        }

        Deque<String> sejarah = sejarahKataLaluanStore.computeIfAbsent(emel, k -> new ArrayDeque<>());
        if (sejarah.isEmpty() && pengguna.getPasswordHash() != null) {
            sejarah.addLast(pengguna.getPasswordHash());
        }
        for (String hashSebelum : sejarah) {
            if (passwordEncoder.matches(kataLaluan, hashSebelum)) {
                throw new IllegalArgumentException(
                        "Kata laluan baharu tidak boleh sama dengan 7 kata laluan terakhir anda");
            }
        }

        String hashBaharu = passwordEncoder.encode(kataLaluan);
        pengguna.setPasswordHash(hashBaharu);
        penggunaRepository.save(pengguna);

        if (sejarah.size() >= 7) {
            sejarah.removeFirst();
        }
        sejarah.addLast(hashBaharu);
    }

    private String normalisasiEmail(String email) {
        String emel = Objects.requireNonNull(email, "E-mel diperlukan").trim().toLowerCase();
        if (emel.isEmpty()) {
            throw new IllegalArgumentException("E-mel diperlukan");
        }
        return emel;
    }

    private boolean mengandungiTigaAksaraBerturutan(String kataLaluan) {
        String teks = kataLaluan.toLowerCase();
        for (int i = 0; i < teks.length() - 2; i++) {
            char a = teks.charAt(i);
            char b = teks.charAt(i + 1);
            char c = teks.charAt(i + 2);
            if (!Character.isLetter(a) || !Character.isLetter(b) || !Character.isLetter(c)) {
                if (Character.isDigit(a) && Character.isDigit(b) && Character.isDigit(c)) {
                    if ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1)) {
                        return true;
                    }
                }
                continue;
            }
            if ((b == a + 1 && c == b + 1) || (b == a - 1 && c == b - 1)) {
                return true;
            }
        }
        return false;
    }

    private String janaKataLaluan(int panjang) {
        StringBuilder sb = new StringBuilder(panjang);
        for (int i = 0; i < panjang; i++) {
            int idx = random.nextInt(AKSARA.length());
            sb.append(AKSARA.charAt(idx));
        }
        return sb.toString();
    }
}
