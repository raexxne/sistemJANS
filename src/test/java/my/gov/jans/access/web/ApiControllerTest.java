package my.gov.jans.access.web;

import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.domain.JenisLokasi;
import my.gov.jans.access.domain.Lokasi;
import my.gov.jans.access.domain.Permohonan;
import my.gov.jans.access.domain.PenyeliaLoji;
import my.gov.jans.access.domain.Role;
import my.gov.jans.access.repo.PenggunaRepository;
import my.gov.jans.access.repo.LokasRepository;
import my.gov.jans.access.repo.PermohonanRepository;
import my.gov.jans.access.repo.PenyeliaLojiRepository;
import my.gov.jans.access.service.AkaunService;
import my.gov.jans.access.service.PermohonanAccessCodeService;
import my.gov.jans.access.service.PermohonanService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockHttpSession;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnOnlyStatusFieldsAfterEmailCodeVerification() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        PermohonanAccessCodeService accessCodeService = mock(PermohonanAccessCodeService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setNomborPermohonan("JAS-2026-000001");
        permohonan.setStatus(my.gov.jans.access.domain.StatusPermohonan.PAS_DIKELUARKAN);
        permohonan.setLocationType(JenisLokasi.LOJI);
        permohonan.setLocationName("Loji Ujian");
        permohonan.setEmailWakil("pemohon@example.com");
        permohonan.setPassToken("private-pass-token");
        when(accessCodeService.sahkanKod("JAS-2026-000001", "123456")).thenReturn(permohonan);
        when(permohonanService.cari("JAS-2026-000001")).thenReturn(permohonan);

        ApiController controller = new ApiController(permohonanService, accessCodeService, akaunService,
                penggunaRepository, lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);
        MockHttpSession session = new MockHttpSession();
        ResponseEntity<?> response = controller.sahkanKodSemakan(
                "JAS-2026-000001", Map.of("kod", "123456"), session);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> status = (Map<String, Object>) response.getBody();
        assertNotNull(status);
        assertEquals(my.gov.jans.access.domain.StatusPermohonan.PAS_DIKELUARKAN, status.get("status"));
        assertEquals("private-pass-token", status.get("passToken"));
        assertEquals("JAS-2026-000001", session.getAttribute("verifiedApplicationNumber"));
        org.junit.jupiter.api.Assertions.assertFalse(status.containsKey("emailWakil"));
        org.junit.jupiter.api.Assertions.assertFalse(status.containsKey("pelawat"));

        ResponseEntity<?> statusSemakan = controller.semak("JAS-2026-000001", null, session);
        assertEquals(HttpStatus.OK, statusSemakan.getStatusCode());
    }

    @Test
    void shouldAllowApplicationLookupByNumberWithoutEmailCode() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setNomborPermohonan("JAS-2026-000001");
        permohonan.setStatus(my.gov.jans.access.domain.StatusPermohonan.DIHANTAR);
        permohonan.setLocationType(JenisLokasi.LOJI);
        permohonan.setLocationName("Loji Ujian");
        when(permohonanService.cari("JAS-2026-000001")).thenReturn(permohonan);
        ApiController controller = new ApiController(permohonanService,
                mock(PermohonanAccessCodeService.class), mock(AkaunService.class),
                mock(PenggunaRepository.class), mock(LokasRepository.class),
                mock(PermohonanRepository.class), mock(PenyeliaLojiRepository.class),
                mock(PasswordEncoder.class));

        ResponseEntity<?> response = controller.semak(
                "JAS-2026-000001", null, new MockHttpSession());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(permohonanService).cari("JAS-2026-000001");
    }

    @Test
    void shouldRequireEmailVerificationBeforeDownloadingApplicationPdf() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        ApiController controller = new ApiController(permohonanService,
                mock(PermohonanAccessCodeService.class), mock(AkaunService.class),
                mock(PenggunaRepository.class), mock(LokasRepository.class),
                mock(PermohonanRepository.class), mock(PenyeliaLojiRepository.class),
                mock(PasswordEncoder.class));

        ResponseEntity<?> response = controller.pdf(
                "JAS-2026-000001", null, new MockHttpSession());

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(permohonanService, never()).cari(any(String.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldIncludeAssignedDistrictsInSupervisorProfile() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        Pengguna pengguna = new Pengguna();
        pengguna.setId(8L);
        pengguna.setName("Penyelia Loji");
        pengguna.setEmail("penyelia@jans.gov.my");
        pengguna.setRole(Role.PENYELIA_LOJI);
        PenyeliaLoji penyelia = new PenyeliaLoji();
        penyelia.setPengguna(pengguna);
        penyelia.setDaerahSeliaan(List.of("Kota Kinabalu", "Penampang"));
        when(penggunaRepository.findByEmail(pengguna.getEmail())).thenReturn(Optional.of(pengguna));
        when(penyeliaLojiRepository.findByPengguna(pengguna)).thenReturn(Optional.of(penyelia));

        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);
        Authentication auth = new TestingAuthenticationToken(
                pengguna.getEmail(),
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_PENYELIA_LOJI")));

        ResponseEntity<?> response = controller.getProfile(auth);
        Map<String, Object> profile = (Map<String, Object>) response.getBody();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(profile);
        assertEquals(List.of("Kota Kinabalu", "Penampang"), profile.get("daerah"));
    }

    @Test
    void shouldReturnAllApplicationsWhenStaffStatusIsNotProvided() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);
        controller.staf(null);

        verify(permohonanService).senaraiSemua();
    }

    @Test
    @SuppressWarnings("null")
    void shouldUpdateEmailWhenProfileIsUpdated() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        Pengguna pengguna = new Pengguna();
        pengguna.setEmail("old@example.com");
        when(penggunaRepository.findByEmail("old@example.com")).thenReturn(Optional.of(pengguna));
        when(penggunaRepository.save(any(Pengguna.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);
        Authentication auth = new TestingAuthenticationToken(
                "old@example.com",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_STAFF")));

        Map<String, String> body = new HashMap<>();
        body.put("email", "new@example.com");

        ResponseEntity<?> response = controller.updateProfile(body, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("new@example.com", pengguna.getEmail());
        verify(penggunaRepository).save(pengguna);
    }

    @Test
    void shouldClearUserReferencesBeforeDeletingUser() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(penggunaRepository.existsById(7L)).thenReturn(true);

        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);

        ResponseEntity<?> response = controller.padamPenggunaAdmin(7L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(permohonanRepository).clearReviewedBy(7L);
        verify(permohonanRepository).clearDecidedBy(7L);
        verify(permohonanRepository).clearCompletedBy(7L);
        verify(penyeliaLojiRepository).deleteByPengguna_Id(7L);
        verify(penggunaRepository).deleteById(7L);
    }

    @Test
    void shouldStoreUploadedPdfWithApplication() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        byte[] kandunganPdf = "%PDF-1.7".getBytes(StandardCharsets.US_ASCII);
        Permohonan permohonan = new Permohonan();
        permohonan.setEmailWakil("pemohon@example.com");
        permohonan.setLocationType(JenisLokasi.LOJI);
        permohonan.setLocationName("Loji Ujian");
        when(lokasRepository.findByType(JenisLokasi.LOJI))
                .thenReturn(List.of(new Lokasi(JenisLokasi.LOJI, "Loji Ujian", "Papar")));
        when(permohonanService.cipta(any(Permohonan.class))).thenAnswer(invocation -> {
            Permohonan disimpan = invocation.getArgument(0);
            disimpan.setNomborPermohonan("JAS-2026-000001");
            return disimpan;
        });

        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);
        MockMultipartFile dokumen = new MockMultipartFile(
                "dokumenSokongan", "surat.pdf", "application/pdf", kandunganPdf);

        ResponseEntity<?> response = controller.mohonDenganDokumen(permohonan, dokumen);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(kandunganPdf, permohonan.getDokumenSokongan());
        assertEquals("surat.pdf", permohonan.getDokumenSokonganNama());
        verify(permohonanService).cipta(permohonan);
    }

    @Test
    void shouldRejectApplicationForLocationNotInAdminList() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setLocationType(JenisLokasi.LOJI);
        permohonan.setLocationName("Lokasi Tidak Wujud");
        MockMultipartFile dokumen = new MockMultipartFile(
                "dokumenSokongan", "surat.pdf", "application/pdf",
                "%PDF-1.7".getBytes(StandardCharsets.US_ASCII));
        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);

        ResponseEntity<?> response = controller.mohonDenganDokumen(permohonan, dokumen);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(permohonanService, never()).cipta(any(Permohonan.class));
    }

    @Test
    void shouldRejectNonPdfContentForSupportDocument() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setLocationType(JenisLokasi.LOJI);
        permohonan.setLocationName("Loji Ujian");
        when(lokasRepository.findByType(JenisLokasi.LOJI))
                .thenReturn(List.of(new Lokasi(JenisLokasi.LOJI, "Loji Ujian", "Papar")));
        MockMultipartFile dokumen = new MockMultipartFile(
                "dokumenSokongan", "surat.pdf", "application/pdf", "bukan PDF".getBytes(StandardCharsets.US_ASCII));
        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);

        ResponseEntity<?> response = controller.mohonDenganDokumen(permohonan, dokumen);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(permohonanService, never()).cipta(any(Permohonan.class));
    }

    @Test
    void shouldRequireSupportDocumentForApplication() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Permohonan permohonan = new Permohonan();
        permohonan.setLocationType(JenisLokasi.LOJI);
        permohonan.setLocationName("Loji Ujian");
        when(lokasRepository.findByType(JenisLokasi.LOJI))
                .thenReturn(List.of(new Lokasi(JenisLokasi.LOJI, "Loji Ujian", "Papar")));
        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);

        ResponseEntity<?> response = controller.mohon(permohonan);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(permohonanService, never()).cipta(any(Permohonan.class));
    }

    @Test
    void shouldSetInlineAndAttachmentDispositionForSupportDocument() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PermohonanRepository permohonanRepository = mock(PermohonanRepository.class);
        PenyeliaLojiRepository penyeliaLojiRepository = mock(PenyeliaLojiRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        Permohonan permohonan = new Permohonan();
        permohonan.setDokumenSokonganNama("sokongan.pdf");
        permohonan.setDokumenSokongan("%PDF-1.4".getBytes(StandardCharsets.US_ASCII));
        when(permohonanRepository.findById(12L)).thenReturn(Optional.of(permohonan));

        ApiController controller = new ApiController(permohonanService, mock(PermohonanAccessCodeService.class),
                akaunService, penggunaRepository,
                lokasRepository, permohonanRepository, penyeliaLojiRepository, passwordEncoder);

        ResponseEntity<?> inline = controller.muatDokumenSokongan(12L, "inline");
        ResponseEntity<?> attachment = controller.muatDokumenSokongan(12L, "attachment");

        assertEquals(HttpStatus.OK, inline.getStatusCode());
        assertTrue(inline.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("inline"));
        assertTrue(attachment.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION).startsWith("attachment"));
    }
}