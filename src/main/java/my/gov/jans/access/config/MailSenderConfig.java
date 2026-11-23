package my.gov.jans.access.config;

import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailSenderConfig {
    @Bean
    @ConfigurationProperties("spring.mail")
    MailProperties mailProperties() {
        return new MailProperties();
    }

    @Bean
    JavaMailSender javaMailSender(MailProperties mailProperties, Environment environment) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(environment.getProperty("SMTP_HOST", mailProperties.getHost()));
        mailSender.setPort(environment.getProperty("SMTP_PORT", Integer.class, mailProperties.getPort()));
        mailSender.setUsername(environment.getProperty("SMTP_USERNAME", mailProperties.getUsername()));
        mailSender.setPassword(environment.getProperty("SMTP_PASSWORD", mailProperties.getPassword()));
        mailSender.setProtocol(mailProperties.getProtocol());
        mailSender.setDefaultEncoding(mailProperties.getDefaultEncoding().name());
        mailSender.getJavaMailProperties().putAll(mailProperties.getProperties());
        return mailSender;
    }
}
