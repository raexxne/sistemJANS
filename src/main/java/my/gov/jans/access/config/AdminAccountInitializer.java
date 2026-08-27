package my.gov.jans.access.config;

import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.domain.Role;
import my.gov.jans.access.repo.PenggunaRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminAccountInitializer {

    @Bean
    CommandLineRunner initAdminAccount(PenggunaRepository penggunaRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            boolean hasAdmin = penggunaRepository.findAll().stream().anyMatch(p -> p.getRole() == Role.ADMIN);
            if (hasAdmin) {
                return;
            }

            Pengguna admin = new Pengguna();
            admin.setName("Administrator");
            admin.setEmail("admin@jans.gov.my");
            admin.setPasswordHash(passwordEncoder.encode("Admin123!"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            penggunaRepository.save(admin);
        };
    }
}
