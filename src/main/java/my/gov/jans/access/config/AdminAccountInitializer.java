package my.gov.jans.access.config;

import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.domain.Role;
import my.gov.jans.access.repo.PenggunaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

@Configuration
public class AdminAccountInitializer {

    @Bean
    CommandLineRunner initAdminAccount(
            PenggunaRepository penggunaRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String adminEmail,
            @Value("${app.bootstrap-admin.password:}") String adminPassword) {
        return args -> {
            if (penggunaRepository.existsByRole(Role.ADMIN)) {
                return;
            }

            if (!StringUtils.hasText(adminEmail) || !StringUtils.hasText(adminPassword)) {
                throw new IllegalStateException(
                        "Tiada akaun ADMIN. Tetapkan APP_BOOTSTRAP_ADMIN_EMAIL dan APP_BOOTSTRAP_ADMIN_PASSWORD untuk mencipta akaun pertama.");
            }
            if (adminPassword.length() < 16) {
                throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_PASSWORD mesti sekurang-kurangnya 16 aksara.");
            }

            Pengguna admin = new Pengguna();
            admin.setName("Administrator");
            admin.setEmail(adminEmail.trim().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            penggunaRepository.save(admin);
        };
    }
}
