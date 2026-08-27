package my.gov.jans.access.config;

import my.gov.jans.access.repo.PenggunaRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        UserDetailsService users(PenggunaRepository repo) {
                return email -> repo.findByEmail(email)
                                .map(u -> User.withUsername(u.getEmail()).password(u.getPasswordHash())
                                                .roles(u.getRole().name())
                                                .disabled(!u.isEnabled()).build())
                                .orElseThrow(() -> new UsernameNotFoundException("Pengguna tidak ditemui"));
        }

        @Bean
        SecurityFilterChain security(HttpSecurity h) throws Exception {
                return h.csrf(c -> c.disable())
                                .authorizeHttpRequests(a -> a
                                                .requestMatchers("/", "/index.html", "/login.html",
                                                                "/forgot-password.html", "/forgot-password-verify.html",
                                                                "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/faq.html", "/hubungi.html", "/aduan.html")
                                                .permitAll()
                                                .requestMatchers("/mohon.html", "/semak.html", "/sah.html").permitAll()
                                                .requestMatchers("/api/public/**", "/api/pas/**").permitAll()
                                                .requestMatchers("/api/staff/**", "/petugas.html")
                                                .hasRole("STAFF")
                                                .requestMatchers("/api/pengarah/**", "/pengarah.html")
                                                .hasRole("PENGARAH")
                                                .requestMatchers("/api/admin/**", "/admin.html")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/profile/**")
                                                .hasAnyRole("STAFF", "PENGARAH", "ADMIN")
                                                .anyRequest().authenticated())
                                .formLogin(f -> f.loginPage("/login.html").loginProcessingUrl("/login")
                                                .usernameParameter("username")
                                                .passwordParameter("password").defaultSuccessUrl("/utama", true)
                                                .failureUrl("/login.html?error=true").permitAll())
                                .logout(l -> l.logoutSuccessUrl("/index.html")).build();
        }
}
