package my.gov.jans.access.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MailSenderConfigTest {
    @Test
    void shouldPreferSmtpEnvironmentVariablesToLocalMailProperties() {
        MailProperties properties = new MailProperties();
        properties.setHost("old.smtp.test");
        properties.setPort(2525);
        properties.setUsername("old-user");
        properties.setPassword("old-password");
        properties.getProperties().put("mail.smtp.auth", "true");

        MockEnvironment environment = new MockEnvironment()
                .withProperty("SMTP_HOST", "smtp.test")
                .withProperty("SMTP_PORT", "587")
                .withProperty("SMTP_USERNAME", "mail-user")
                .withProperty("SMTP_PASSWORD", "test-password");

        JavaMailSenderImpl mailSender = (JavaMailSenderImpl) new MailSenderConfig()
                .javaMailSender(properties, environment);

        assertEquals("smtp.test", mailSender.getHost());
        assertEquals(587, mailSender.getPort());
        assertEquals("mail-user", mailSender.getUsername());
        assertEquals("test-password", mailSender.getPassword());
        assertEquals("true", mailSender.getJavaMailProperties().getProperty("mail.smtp.auth"));
    }
}
