package my.gov.jans.access.service;

import my.gov.jans.access.domain.JenisLokasi;
import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.domain.Pelawat;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PermohonanServiceTest {

    @Test
    void shouldRequireVehicleRegistrationForEveryVisitorOnNewApplication() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setEmailWakil("applicant@example.com");
        permohonan.tambahPelawat(new Pelawat());

        PermohonanService service = new PermohonanService(
                repo, mock(PenggunaRepository.class), mock(EmailService.class));

        assertThrows(IllegalArgumentException.class, () -> service.cipta(permohonan));
        verify(repo, never()).save(any(Permohonan.class));
    }

    @Test
    void shouldNotifyOnlyApplicantWhenApplicationIsCreated() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setEmailWakil("applicant@example.com");
        Pelawat pelawat = new Pelawat();
        pelawat.setNoPendaftaranKenderaan("ABC123");
        permohonan.tambahPelawat(pelawat);
        when(repo.save(any(Permohonan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);
        service.cipta(permohonan);

        verify(emailService).hantarEmail(eq("applicant@example.com"), anyString(), anyString(), isNull(), isNull());
        verify(penggunaRepository, never()).findByRole(Role.PENGARAH);
    }

    @Test
    @SuppressWarnings("null")
    void shouldSendEmailToDirectorEvenWhenDirectorAccountIsDisabled() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);

        Permohonan permohonan = new Permohonan();
        permohonan.setId(1L);
        permohonan.setStatus(StatusPermohonan.DIHANTAR);
        permohonan.setNomborPermohonan("JAS-2026-000001");
        permohonan.setEmailWakil("applicant@example.com");
        Pelawat pelawat = new Pelawat();
        pelawat.setNamaPenuh("Ali Bin Abu");
        pelawat.setNoKadPengenalan("900101-01-0001");
        pelawat.setEmail("applicant@example.com");
        pelawat.setNoTelefonBimbit("0123456789");
        pelawat.setJawatan("Pegawai");
        permohonan.tambahPelawat(pelawat);

        Pengguna staff = new Pengguna();
        staff.setEmail("staff@example.com");

        Pengguna director = new Pengguna();
        director.setEmail("director@example.com");
        director.setRole(Role.PENGARAH);
        director.setEnabled(false);

        when(repo.findById(1L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(penggunaRepository.findByRole(Role.PENGARAH)).thenReturn(List.of(director));
        when(repo.save(any(Permohonan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);
        service.publicUrl = "https://example.com";

        Permohonan result = service.hantarPengarah(1L, "Catatan semakan", "staff@example.com");

        assertEquals(StatusPermohonan.MENUNGGU_PENGARAH, result.getStatus());
        assertEquals("Catatan semakan", result.getStaffNote());
        var order = inOrder(repo, emailService);
        order.verify(repo).save(permohonan);
        order.verify(repo).flush();
        order.verify(emailService).hantarEmail(
                eq("director@example.com"),
                contains("JAS-2026-000001"),
                contains("https://example.com/login.html"),
                isNull(),
                isNull());
    }

    @Test
    @SuppressWarnings("null")
    void shouldNotNotifyDirectorWhenSavingSubmissionFails() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setStatus(StatusPermohonan.DIHANTAR);
        when(repo.findById(8L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(new Pengguna()));
        doThrow(new IllegalStateException("Database unavailable")).when(repo).flush();

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);

        assertThrows(IllegalStateException.class,
                () -> service.hantarPengarah(8L, "Semakan lengkap", "staff@example.com"));
        verify(emailService, never()).hantarEmail(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @SuppressWarnings("null")
    void shouldFailSubmittingToDirectorWhenNotificationEmailFails() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setId(6L);
        permohonan.setStatus(StatusPermohonan.DIHANTAR);
        permohonan.setNomborPermohonan("JAS-2026-000006");

        Pengguna staff = new Pengguna();
        staff.setEmail("staff@example.com");
        Pengguna director = new Pengguna();
        director.setEmail("director@example.com");
        director.setRole(Role.PENGARAH);

        when(repo.findById(6L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(penggunaRepository.findByRole(Role.PENGARAH)).thenReturn(List.of(director));
        doThrow(new IllegalStateException("SMTP unavailable")).when(emailService).hantarEmail(
                eq("director@example.com"), anyString(), anyString(), isNull(), isNull());

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> service.hantarPengarah(6L, "Semakan lengkap", "staff@example.com"));

        assertEquals("E-mel notifikasi gagal dihantar. Semak alamat e-mel pengarah dan konfigurasi SMTP.",
                error.getMessage());
    }

    @Test
    @SuppressWarnings("null")
    void shouldFailSubmittingToDirectorWhenNoDirectorEmailIsRegistered() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setId(7L);
        permohonan.setStatus(StatusPermohonan.DIHANTAR);
        permohonan.setNomborPermohonan("JAS-2026-000007");

        Pengguna staff = new Pengguna();
        staff.setEmail("staff@example.com");

        when(repo.findById(7L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(penggunaRepository.findByRole(Role.PENGARAH)).thenReturn(List.of());

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);

        assertThrows(IllegalStateException.class,
                () -> service.hantarPengarah(7L, "Semakan lengkap", "staff@example.com"));
        verify(emailService, never()).hantarEmail(anyString(), anyString(), anyString(), any(), any());
    }

    @Test
    @SuppressWarnings("null")
    void shouldDefaultStaffNoteWhenSubmittingToDirectorWithoutNote() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);

        Permohonan permohonan = new Permohonan();
        permohonan.setId(2L);
        permohonan.setStatus(StatusPermohonan.DIHANTAR);
        permohonan.setNomborPermohonan("JAS-2026-000002");

        Pengguna staff = new Pengguna();
        staff.setEmail("staff@example.com");

        Pengguna director = new Pengguna();
        director.setEmail("director@example.com");
        director.setRole(Role.PENGARAH);

        when(repo.findById(2L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("staff@example.com")).thenReturn(Optional.of(staff));
        when(penggunaRepository.findByRole(Role.PENGARAH)).thenReturn(List.of(director));
        when(repo.save(any(Permohonan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);

        Permohonan result = service.hantarPengarah(2L, null, "staff@example.com");

        assertEquals(StatusPermohonan.MENUNGGU_PENGARAH, result.getStatus());
        assertEquals("Tidak ada catatan", result.getStaffNote());
    }

    @Test
    void shouldEmailApplicantWhenDirectorApprovesApplication() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = permohonanMenungguKeputusan(3L);
        Pengguna director = new Pengguna();
        director.setEmail("director@example.com");

        when(repo.findById(3L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("director@example.com")).thenReturn(Optional.of(director));

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);
        Permohonan result = service.keputusan(3L, true, null, "director@example.com");

        assertEquals(StatusPermohonan.PAS_DIKELUARKAN, result.getStatus());
        verify(emailService).hantarEmail(
                eq("applicant@example.com"),
                contains("Permohonan Diluluskan"),
                contains("diluluskan"),
                isNull(),
                isNull());
    }

    @Test
    void shouldEmailApplicantWhenDirectorRejectsApplication() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = permohonanMenungguKeputusan(4L);
        Pengguna director = new Pengguna();
        director.setEmail("director@example.com");

        when(repo.findById(4L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("director@example.com")).thenReturn(Optional.of(director));

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);
        Permohonan result = service.keputusan(4L, false, "Dokumen tidak lengkap", "director@example.com");

        assertEquals(StatusPermohonan.DITOLAK, result.getStatus());
        verify(emailService).hantarEmail(
                eq("applicant@example.com"),
                contains("Permohonan Ditolak"),
                contains("tidak diluluskan"),
                isNull(),
                isNull());
    }

    @Test
    void shouldKeepDirectorDecisionWhenNotificationEmailFails() {
        PermohonanRepository repo = mock(PermohonanRepository.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        EmailService emailService = mock(EmailService.class);
        Permohonan permohonan = permohonanMenungguKeputusan(5L);
        Pengguna director = new Pengguna();
        director.setEmail("director@example.com");

        when(repo.findById(5L)).thenReturn(Optional.of(permohonan));
        when(penggunaRepository.findByEmail("director@example.com")).thenReturn(Optional.of(director));
        doThrow(new IllegalStateException("SMTP unavailable")).when(emailService).hantarEmail(
                eq("applicant@example.com"), anyString(), anyString(), isNull(), isNull());

        PermohonanService service = new PermohonanService(repo, penggunaRepository, emailService);
        Permohonan result = service.keputusan(5L, false, "Dokumen tidak lengkap", "director@example.com");

        assertEquals(StatusPermohonan.DITOLAK, result.getStatus());
        verify(repo).save(permohonan);
    }

    private Permohonan permohonanMenungguKeputusan(Long id) {
        Permohonan permohonan = new Permohonan();
        permohonan.setId(id);
        permohonan.setNomborPermohonan("JAS-2026-00000" + id);
        permohonan.setEmailWakil("applicant@example.com");
        permohonan.setStatus(StatusPermohonan.MENUNGGU_PENGARAH);
        return permohonan;
    }

    @Test
    void shouldNotIncludeWaitingDirectorStatusInApplicationPdf() throws Exception {
        PermohonanService service = new PermohonanService(mock(PermohonanRepository.class),
                mock(PenggunaRepository.class), mock(EmailService.class));
        Permohonan permohonan = new Permohonan();
        permohonan.setNomborPermohonan("JAS-2026-000002");
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
