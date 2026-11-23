package my.gov.jans.access.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.Objects;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${app.mail.mock:false}")
    private boolean mockMode;

    @Value("${SMTP_USERNAME:${spring.mail.username:}}")
    private String mailUsername;

    @Value("${SMTP_PASSWORD:${spring.mail.password:}}")
    private String mailPassword;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void hantarEmail(String penerima, String subjek, String isi, byte[] lampiran, String namaLampiran) {
        String penerimaBersih = Objects.requireNonNull(penerima, "Penerima email diperlukan").trim();
        if (penerimaBersih.isBlank()) {
            throw new IllegalArgumentException("Penerima email tidak boleh kosong");
        }

        if (mockMode) {
            log.info(" [EMAIL MOCK MODE]");
            log.info("   Kepada: {}", penerimaBersih);
            log.info("   Subjek: {}", subjek);
            log.info("   Isi: {}", isi);
            if (lampiran != null) {
                log.info("   Lampiran: {} ({} bytes)", namaLampiran, lampiran.length);
            }
            log.info(" Email dilog (tanpa dihantar)");
            return;
        }

        if (mailUsername == null || mailUsername.isBlank()) {
            throw new IllegalStateException("SMTP_USERNAME belum dikonfigurasi");
        }
        if (mailPassword == null || mailPassword.isBlank()) {
            throw new IllegalStateException(
                    "SMTP_PASSWORD belum dikonfigurasi. Tetapkan Gmail App Password melalui pemboleh ubah persekitaran SMTP_PASSWORD.");
        }

        RuntimeException kegagalanTerakhir = null;
        for (int percubaan = 1; percubaan <= 2; percubaan++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(penerimaBersih);
                helper.setSubject(Objects.requireNonNull(subjek));
                helper.setText(Objects.requireNonNull(isi), true);
                String daripada = mailUsername;
                if (daripada != null && !daripada.isBlank()) {
                    helper.setFrom(daripada);
                    helper.setReplyTo(daripada);
                }

                if (lampiran != null && namaLampiran != null) {
                    helper.addAttachment(namaLampiran,
                            new org.springframework.core.io.ByteArrayResource(lampiran));
                }

                mailSender.send(message);
                log.info("Email berjaya dihantar kepada {} untuk subjek '{}'", penerimaBersih, subjek);
                return;
            } catch (Exception e) {
                kegagalanTerakhir = new IllegalStateException(
                        "Email gagal dihantar: " + e.getMessage(), e);
                log.warn("Percubaan {}/2 gagal menghantar email kepada {} untuk subjek '{}': {}",
                        percubaan, penerimaBersih, subjek, e.getMessage());
            }
        }

        log.error("Gagal menghantar email selepas 2 percubaan kepada {} untuk subjek '{}'",
                penerimaBersih, subjek);
        throw kegagalanTerakhir;
    }
}
