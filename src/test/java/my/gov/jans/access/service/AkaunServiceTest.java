package my.gov.jans.access.service;

import my.gov.jans.access.domain.PasswordResetToken;
import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.repo.PasswordResetTokenRepository;
import my.gov.jans.access.repo.PenggunaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AkaunServiceTest {

    @Test
    void persistsFailedResetCodeAttempts() {
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        PasswordResetTokenRepository tokenRepository = mock(PasswordResetTokenRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);

        Pengguna pengguna = new Pengguna();
        pengguna.setId(4L);
        pengguna.setEmail("user@example.com");
        PasswordResetToken token = new PasswordResetToken(
                pengguna, "stored-hash", Instant.now().plusSeconds(600), Instant.now());

        when(penggunaRepository.findByEmail("user@example.com")).thenReturn(Optional.of(pengguna));
        when(tokenRepository.findByUserIdForUpdate(4L)).thenReturn(Optional.of(token));
        when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(false);

        AkaunService service = new AkaunService(
                penggunaRepository, tokenRepository, passwordEncoder, emailService);

        assertThrows(IllegalStateException.class,
                () -> service.sahkanKodDanResetKataLaluan("user@example.com", "123456"));

        assertEquals(1, token.getFailedAttempts());
        verify(tokenRepository).save(token);
    }
}
