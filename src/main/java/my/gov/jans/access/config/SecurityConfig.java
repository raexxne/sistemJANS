package my.gov.jans.access.config;

import my.gov.jans.access.repo.PenggunaRepository;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

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
                CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                csrfTokenRepository.setCookiePath("/");
                return h.csrf(c -> c
                                .csrfTokenRepository(csrfTokenRepository)
                                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                                .authorizeHttpRequests(a -> a
                                                .requestMatchers("/", "/index", "/index.html", "/login", "/login.html", "/forgot-password", "/forgot-password-verify",
                                                                "/forgot-password.html", "/forgot-password-verify.html",
                                                                "/css/**", "/js/**", "/images/**")
                                                .permitAll()
                                                .requestMatchers("/faq", "/hubungi", "/aduan", "/faq.html", "/hubungi.html", "/aduan.html")
                                                .permitAll()
                                                .requestMatchers("/mohon", "/semak", "/sah", "/mohon.html", "/semak.html", "/sah.html").permitAll()
                                                .requestMatchers("/api/csrf", "/api/public/**", "/api/pas/**")
                                                .permitAll()
                                                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                                                .requestMatchers("/api/staff/**", "/petugas", "/petugas.html")
                                                .hasRole("STAFF")
                                                .requestMatchers("/api/pengarah/**", "/pengarah", "/pengarah.html")
                                                .hasRole("PENGARAH")
                                                .requestMatchers("/api/penyelia/**", "/penyelia-loji", "/penyelia-loji.html")
                                                .hasRole("PENYELIA_LOJI")
                                                .requestMatchers("/api/admin/**", "/admin", "/admin.html")
                                                .hasRole("ADMIN")
                                                .requestMatchers("/api/profile/**")
                                                .hasAnyRole("STAFF", "PENGARAH", "ADMIN", "PENYELIA_LOJI")
                                                .anyRequest().authenticated())
                                .formLogin(f -> f.loginPage("/login.html").loginProcessingUrl("/login")
                                                .usernameParameter("username")
                                                .passwordParameter("password").defaultSuccessUrl("/utama", true)
                                                .failureUrl("/login.html?error=true").permitAll())
                                .logout(l -> l.logoutSuccessUrl("/index.html")).build();
        }
}
