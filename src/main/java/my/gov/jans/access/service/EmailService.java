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

    @Value("${spring.mail.username:}")
    private String mailUsername;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void hantarEmail(String penerima, String subjek, String isi, byte[] lampiran, String namaLampiran) {
        try {
            if (mockMode) {
                // Mode ujian - log sahaja
                log.info(" [EMAIL MOCK MODE]");
                log.info("   Kepada: {}", penerima);
                log.info("   Subjek: {}", subjek);
                log.info("   Isi: {}", isi);
                if (lampiran != null) {
                    log.info("   Lampiran: {} ({} bytes)", namaLampiran, lampiran.length);
                }
                log.info(" Email dilog (tanpa dihantar)");
            } else {
                // Mode production - hantar sebenarnya
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(Objects.requireNonNull(penerima));
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
                log.info(" Email dihantar kepada: {}", penerima);
            }
        } catch (Exception e) {
            log.error(" Gagal hantar email: {}", e.getMessage(), e);
            throw new IllegalStateException("Email gagal dihantar: " + e.getMessage(), e);
        }
    }
}
