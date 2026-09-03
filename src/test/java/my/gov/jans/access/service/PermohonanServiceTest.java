package my.gov.jans.access.service;

import my.gov.jans.access.domain.JenisLokasi;
import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.domain.Permohonan;
import my.gov.jans.access.domain.Role;
import my.gov.jans.access.domain.StatusPermohonan;
import my.gov.jans.access.repo.PenggunaRepository;
import my.gov.jans.access.repo.PermohonanRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermohonanServiceTest {

    @Test
    @SuppressWarnings("null")
    void shouldSendEmailToDirectorWhenSubmittingToDirector() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);

        Permohonan permohonan = new Permohonan();
        permohonan.setId(1L);
        permohonan.setStatus(StatusPermohonan.DIHANTAR);
        permohonan.setNomborPermohonan("JAS-2026-000001");
        permohonan.setEmail("applicant@example.com");

        Pengguna staff = new Pengguna();
        staff.setEmail("staff@example.com");

        Pengguna director = new Pengguna();
        director.setEmail("director@example.com");
        director.setRole(Role.PENGARAH);

        when(repo.findById(1L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(penggunaRepository.findByRole(Role.PENGARAH)).thenReturn(List.of(director));
        when(repo.save(any(Permohonan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);
        service.publicUrl = "https://example.com";

        Permohonan result = service.hantarPengarah(1L, "Catatan semakan", "staff@example.com");

        assertEquals(StatusPermohonan.MENUNGGU_PENGARAH, result.getStatus());
        verify(emailService).hantarEmail(
                eq("director@example.com"),
                contains("Permohonan"),
                contains("sini"),
                isNull(),
                isNull());
    }

    @Test
    void shouldNotIncludeWaitingDirectorStatusInApplicationPdf() throws Exception {
        PermohonanService service = new PermohonanService(mock(PermohonanRepository.class),
                mock(PenggunaRepository.class), mock(EmailService.class));
        Permohonan permohonan = new Permohonan();
        permohonan.setNomborPermohonan("JAS-2026-000002");
        permohonan.setApplicantName("Ali Bin Abu");
        permohonan.setIcNo("900101-01-0001");
        permohonan.setEmail("applicant@example.com");
        permohonan.setJawatanGred("Pegawai");
        permohonan.setPhoneMobile("0123456789");
        permohonan.setPhoneOffice("088123456");
        permohonan.setOrganisation("JAS");
        permohonan.setApplicationDate(LocalDate.of(2026, 8, 3));
        permohonan.setVisitDate(LocalDate.of(2026, 8, 4));
        permohonan.setVisitTime(LocalTime.of(9, 30));
        permohonan.setLocationType(JenisLokasi.LOJI);
        permohonan.setLocationName("Pejabat JAS");
        permohonan.setPurpose("Lawatan");
        permohonan.setStatus(StatusPermohonan.MENUNGGU_PENGARAH);

        byte[] pdf = service.pdfPermohonan(permohonan);
        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        assertFalse(text.contains("MENUNGGU PENGARAH"));
        assertFalse(text.contains("Status"));
    }

    @Test
    void shouldNormalizeJasApplicationNumberBeforeSearching() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setNomborPermohonan("JAS-2026-000001");
        when(repo.findByNomborPermohonan("JAS-2026-000001")).thenReturn(Optional.of(permohonan));

        PermohonanService service = new PermohonanService(repo, mock(PenggunaRepository.class),
                mock(EmailService.class));

        assertEquals(permohonan, service.cari(" jas-2026-000001 "));
    }
}
