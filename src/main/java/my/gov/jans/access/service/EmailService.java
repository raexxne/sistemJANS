package my.gov.jans.access.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;
import java.util.Objects;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TetapanSistemService settings;

    @Value("${app.mail.mock:false}")
    private boolean mockMode;

    @Value("${SMTP_USERNAME:${spring.mail.username:}}")
    private String mailUsername;

    @Value("${SMTP_PASSWORD:${spring.mail.password:}}")
    private String mailPassword;

    public EmailService(JavaMailSender mailSender, TetapanSistemService settings) {
        this.mailSender = mailSender;
        this.settings = settings;
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

        SmtpSettings smtp = smtpSettings();
        if (smtp.username() == null || smtp.username().isBlank()) {
            throw new IllegalStateException("SMTP_USERNAME belum dikonfigurasi");
        }
        if (smtp.password() == null || smtp.password().isBlank()) {
            throw new IllegalStateException(
                    "SMTP_PASSWORD belum dikonfigurasi. Tetapkan Gmail App Password melalui pemboleh ubah persekitaran SMTP_PASSWORD.");
        }

        RuntimeException kegagalanTerakhir = null;
        for (int percubaan = 1; percubaan <= 2; percubaan++) {
            try {
                JavaMailSender activeSender = activeSender(smtp);
                MimeMessage message = activeSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

                helper.setTo(penerimaBersih);
                helper.setSubject(Objects.requireNonNull(subjek));
                helper.setText(Objects.requireNonNull(isi), true);
                String daripada = smtp.username();
                if (daripada != null && !daripada.isBlank()) {
                    helper.setFrom(daripada);
                    helper.setReplyTo(daripada);
                }

                if (lampiran != null && namaLampiran != null) {
                    helper.addAttachment(namaLampiran,
                            new org.springframework.core.io.ByteArrayResource(lampiran));
                }

                activeSender.send(message);
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

    public void hantarEmailTemplate(String templateKey, Map<String, String> variables, String penerima,
            String fallbackSubject, String fallbackBody, byte[] attachment, String attachmentName) {
        RenderedEmail rendered = renderEmailTemplate(templateKey, variables, fallbackSubject, fallbackBody);
        if (rendered != null && !rendered.enabled()) {
            return;
        }
        hantarEmail(penerima,
                rendered == null ? fallbackSubject : rendered.subject(),
                rendered == null ? fallbackBody : rendered.body(),
                attachment, attachmentName);
    }

    public RenderedEmail renderEmailTemplate(String templateKey, Map<String, String> variables,
            String fallbackSubject, String fallbackBody) {
        Map<String, Object> template = settings.find(TetapanSistemService.TEMPLATE, templateKey);
        if (Boolean.FALSE.equals(template.get("enabled"))) {
            log.info("Template e-mel {} dilumpuhkan; penghantaran dilangkau", templateKey);
            return new RenderedEmail("", "", false);
        }
        String subject = render(Objects.toString(template.getOrDefault("subject", fallbackSubject)), variables);
        String body = render(Objects.toString(template.getOrDefault("body", fallbackBody)), variables);
        return new RenderedEmail(subject, body, true);
    }

    private String render(String value, Map<String, String> variables) {
        String rendered = value == null ? "" : value;
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace("{{" + variable.getKey() + "}}",
                    Objects.toString(variable.getValue(), ""));
        }
        return rendered;
    }

    private SmtpSettings smtpSettings() {
        Map<String, Object> configured = settings.find(TetapanSistemService.SMTP, "primary");
        String username = value(configured, "username", mailUsername);
        String password = value(configured, "password", mailPassword);
        String host = value(configured, "host", null);
        int port;
        try {
            port = Integer.parseInt(value(configured, "port", "0"));
        } catch (NumberFormatException e) {
            port = 0;
        }
        boolean auth = Boolean.parseBoolean(value(configured, "auth", "true"));
        boolean startTls = Boolean.parseBoolean(value(configured, "startTls", "true"));
        return new SmtpSettings(host, port, username, password, auth, startTls);
    }

    private JavaMailSender activeSender(SmtpSettings smtp) {
        if (!(mailSender instanceof JavaMailSenderImpl fallback)) {
            return mailSender;
        }
        JavaMailSenderImpl configured = new JavaMailSenderImpl();
        configured.setHost(smtp.host() == null || smtp.host().isBlank() ? fallback.getHost() : smtp.host());
        configured.setPort(smtp.port() > 0 ? smtp.port() : fallback.getPort());
        configured.setUsername(smtp.username());
        configured.setPassword(smtp.password());
        configured.setProtocol(fallback.getProtocol());
        configured.setDefaultEncoding(Objects.toString(fallback.getDefaultEncoding(), "UTF-8"));
        configured.getJavaMailProperties().putAll(fallback.getJavaMailProperties());
        configured.getJavaMailProperties().put("mail.smtp.auth", String.valueOf(smtp.auth()));
        configured.getJavaMailProperties().put("mail.smtp.starttls.enable", String.valueOf(smtp.startTls()));
        return configured;
    }

    private String value(Map<String, Object> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null || String.valueOf(value).isBlank() ? fallback : String.valueOf(value);
    }

    private record SmtpSettings(String host, int port, String username, String password, boolean auth,
            boolean startTls) {
    }

    public record RenderedEmail(String subject, String body, boolean enabled) {
    }
}
