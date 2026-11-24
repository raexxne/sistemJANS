package my.gov.jans.access.service;

import my.gov.jans.access.domain.Permohonan;
import my.gov.jans.access.domain.PermohonanAccessCode;
import my.gov.jans.access.repo.PermohonanAccessCodeRepository;
import my.gov.jans.access.repo.PermohonanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class PermohonanAccessCodeService {
    private static final Logger LOG = LoggerFactory.getLogger(PermohonanAccessCodeService.class);
    private static final long CODE_VALIDITY_SECONDS = 600;
    private static final long REQUEST_COOLDOWN_SECONDS = 60;
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final PermohonanRepository permohonanRepository;
    private final PermohonanAccessCodeRepository accessCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public PermohonanAccessCodeService(
            PermohonanRepository permohonanRepository,
            PermohonanAccessCodeRepository accessCodeRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {
        this.permohonanRepository = permohonanRepository;
        this.accessCodeRepository = accessCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    @Transactional
    public void mintaKod(String nomborPermohonan) {
        String nomborBersih = normalisasiNombor(nomborPermohonan);
        Permohonan permohonan = permohonanRepository.findByNomborPermohonan(nomborBersih).orElse(null);
        if (permohonan == null || permohonan.getEmailWakil() == null || permohonan.getEmailWakil().isBlank()) {
            return;
        }

        Long applicationId = Objects.requireNonNull(permohonan.getId(), "ID permohonan diperlukan");
        Instant sekarang = Instant.now();
        Optional<PermohonanAccessCode> sediaAda =
                accessCodeRepository.findByApplicationIdForUpdate(applicationId);
        if (sediaAda.isPresent()
                && sediaAda.get().getCreatedAt().isAfter(sekarang.minusSeconds(REQUEST_COOLDOWN_SECONDS))) {
            return;
        }

        String kod = String.format("%06d", random.nextInt(1_000_000));
        PermohonanAccessCode accessCode = new PermohonanAccessCode(
                applicationId,
                passwordEncoder.encode(kod),
                sekarang.plusSeconds(CODE_VALIDITY_SECONDS),
                sekarang);
        accessCodeRepository.save(accessCode);

        String isi = "<p>Tuan/Puan,</p>"
                + "<p>Kod pengesahan untuk melihat status permohonan JAS anda ialah:</p>"
                + "<p style=\"font-size: 1.4rem; font-weight: 700; letter-spacing: 0.12em;\">" + kod + "</p>"
                + "<p>Kod ini sah selama 10 minit. Jangan kongsi kod ini dengan orang lain.</p>"
                + "<p>Jika anda tidak meminta kod ini, sila abaikan e-mel ini.</p>";
        try {
            hantarEmailDenganTemplate(
                    "application-access-code",
                    Map.of("code", kod),
                    permohonan.getEmailWakil(),
                    "Kod Pengesahan Semakan Permohonan JAS",
                    isi,
                    null,
                    null);
        } catch (RuntimeException e) {
            LOG.error("Kod pengesahan untuk permohonan {} gagal dihantar", nomborBersih, e);
        }
    }

    private void hantarEmailDenganTemplate(String templateKey, Map<String, String> variables, String recipient,
            String fallbackSubject, String fallbackBody, byte[] attachment, String attachmentName) {
        EmailService.RenderedEmail rendered = emailService.renderEmailTemplate(
                templateKey, variables, fallbackSubject, fallbackBody);
        if (rendered != null && !rendered.enabled()) {
            return;
        }
        emailService.hantarEmail(
                recipient,
                rendered == null ? fallbackSubject : rendered.subject(),
                rendered == null ? fallbackBody : rendered.body(),
                attachment,
                attachmentName);
    }

    @Transactional(noRollbackFor = IllegalStateException.class)
    public Permohonan sahkanKod(String nomborPermohonan, String kod) {
        String kodBersih = Optional.ofNullable(kod).orElse("").trim();
        if (!kodBersih.matches("\\d{6}")) {
            throw new IllegalArgumentException("Kod pengesahan mesti mengandungi 6 digit");
        }

        String nomborBersih = normalisasiNombor(nomborPermohonan);
        Permohonan permohonan = permohonanRepository.findByNomborPermohonan(nomborBersih)
                .orElseThrow(() -> new IllegalStateException("Kod pengesahan tidak sah atau telah luput"));
        Long applicationId = Objects.requireNonNull(permohonan.getId(), "ID permohonan diperlukan");
        PermohonanAccessCode accessCode = accessCodeRepository.findByApplicationIdForUpdate(applicationId)
                .orElseThrow(() -> new IllegalStateException("Kod pengesahan tidak sah atau telah luput"));

        if (accessCode.isVerified()
                || Instant.now().isAfter(accessCode.getExpiresAt())
                || accessCode.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new IllegalStateException("Kod pengesahan tidak sah atau telah luput");
        }
        if (!passwordEncoder.matches(kodBersih, accessCode.getCodeHash())) {
            accessCode.incrementFailedAttempts();
            accessCodeRepository.save(accessCode);
            throw new IllegalStateException("Kod pengesahan tidak sah atau telah luput");
        }

        accessCode.markVerified();
        accessCodeRepository.save(accessCode);
        return permohonan;
    }

    private String normalisasiNombor(String nomborPermohonan) {
        String nomborBersih = Optional.ofNullable(nomborPermohonan).orElse("").trim().toUpperCase(Locale.ROOT);
        if (nomborBersih.isEmpty()) {
            throw new IllegalArgumentException("Nombor permohonan diperlukan");
        }
        return nomborBersih;
    }
}
