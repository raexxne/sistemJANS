package my.gov.jans.access.service;

import my.gov.jans.access.domain.Permohonan;
import my.gov.jans.access.domain.PermohonanAccessCode;
import my.gov.jans.access.repo.PermohonanAccessCodeRepository;
import my.gov.jans.access.repo.PermohonanRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermohonanAccessCodeServiceTest {
    @Test
    void shouldStoreHashedCodeAndSendItToRegisteredRepresentative() {
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PermohonanAccessCodeRepository accessCodeRepository = mock(PermohonanAccessCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = permohonan();
        when(permohonanRepository.findByNomborPermohonan("JAS-2026-000001"))
                .thenReturn(Optional.of(permohonan));
        when(accessCodeRepository.findByApplicationIdForUpdate(1L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");

        PermohonanAccessCodeService service = new PermohonanAccessCodeService(
                permohonanRepository, accessCodeRepository, passwordEncoder, emailService);
        service.mintaKod(" jas-2026-000001 ");

        ArgumentCaptor<PermohonanAccessCode> savedCode = ArgumentCaptor.forClass(PermohonanAccessCode.class);
        verify(accessCodeRepository).save(savedCode.capture());
        assertEquals("hashed-code", savedCode.getValue().getCodeHash());
        verify(emailService).hantarEmail(
                eq("applicant@example.com"),
                eq("Kod Pengesahan Semakan Permohonan JAS"),
                anyString(),
                eq(null),
                eq(null));
    }

    @Test
    void shouldNotSendCodeForUnknownApplication() {
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PermohonanAccessCodeRepository accessCodeRepository = mock(PermohonanAccessCodeRepository.class);
        EmailService emailService = mock(EmailService.class);
        when(permohonanRepository.findByNomborPermohonan("JAS-2026-000001")).thenReturn(Optional.empty());

        PermohonanAccessCodeService service = new PermohonanAccessCodeService(
                permohonanRepository, accessCodeRepository, mock(PasswordEncoder.class), emailService);
        service.mintaKod("JAS-2026-000001");

        verify(emailService, never()).hantarEmail(anyString(), anyString(), anyString(), any(), any());
        verify(accessCodeRepository, never()).save(any(PermohonanAccessCode.class));
    }

    @Test
    void shouldVerifyCodeAgainstItsHashAndRemoveItAfterUse() {
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PermohonanAccessCodeRepository accessCodeRepository = mock(PermohonanAccessCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Permohonan permohonan = permohonan();
        PermohonanAccessCode accessCode = new PermohonanAccessCode(
                1L, "hashed-code", Instant.now().plusSeconds(600), Instant.now());
        when(permohonanRepository.findByNomborPermohonan("JAS-2026-000001"))
                .thenReturn(Optional.of(permohonan));
        when(accessCodeRepository.findByApplicationIdForUpdate(1L)).thenReturn(Optional.of(accessCode));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        PermohonanAccessCodeService service = new PermohonanAccessCodeService(
                permohonanRepository, accessCodeRepository, passwordEncoder, mock(EmailService.class));

        assertEquals(permohonan, service.sahkanKod("JAS-2026-000001", "123456"));
        assertTrue(accessCode.isVerified());
        verify(accessCodeRepository).save(accessCode);
        assertThrows(IllegalStateException.class, () -> service.sahkanKod("JAS-2026-000001", "123456"));
    }

    @Test
    void shouldCountFailedCodeAttempts() {
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PermohonanAccessCodeRepository accessCodeRepository = mock(PermohonanAccessCodeRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Permohonan permohonan = permohonan();
        PermohonanAccessCode accessCode = new PermohonanAccessCode(
                1L, "hashed-code", Instant.now().plusSeconds(600), Instant.now());
        when(permohonanRepository.findByNomborPermohonan("JAS-2026-000001"))
                .thenReturn(Optional.of(permohonan));
        when(accessCodeRepository.findByApplicationIdForUpdate(1L)).thenReturn(Optional.of(accessCode));
        when(passwordEncoder.matches("000000", "hashed-code")).thenReturn(false);

        PermohonanAccessCodeService service = new PermohonanAccessCodeService(
                permohonanRepository, accessCodeRepository, passwordEncoder, mock(EmailService.class));

        assertThrows(IllegalStateException.class, () -> service.sahkanKod("JAS-2026-000001", "000000"));
        assertEquals(1, accessCode.getFailedAttempts());
        verify(accessCodeRepository).save(accessCode);
    }

    private Permohonan permohonan() {
        Permohonan permohonan = new Permohonan();
        permohonan.setId(1L);
        permohonan.setNomborPermohonan("JAS-2026-000001");
        permohonan.setEmailWakil("applicant@example.com");
        return permohonan;
    }
}
