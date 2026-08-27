package my.gov.jans.access.web;

import my.gov.jans.access.domain.Pengguna;
import my.gov.jans.access.repo.PenggunaRepository;
import my.gov.jans.access.repo.LokasRepository;
import my.gov.jans.access.service.AkaunService;
import my.gov.jans.access.service.PermohonanService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApiControllerTest {

    @Test
    void shouldReturnAllApplicationsWhenStaffStatusIsNotProvided() {
        PermohonanService permohonanService = mock(PermohonanService.class);
        AkaunService akaunService = mock(AkaunService.class);
        PenggunaRepository penggunaRepository = mock(PenggunaRepository.class);
        LokasRepository lokasRepository = mock(LokasRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        ApiController controller = new ApiController(permohonanService, akaunService, penggunaRepository,
                lokasRepository, passwordEncoder);
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
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        Pengguna pengguna = new Pengguna();
        pengguna.setEmail("old@example.com");
        when(penggunaRepository.findByEmail("old@example.com")).thenReturn(Optional.of(pengguna));
        when(penggunaRepository.save(any(Pengguna.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ApiController controller = new ApiController(permohonanService, akaunService, penggunaRepository,
                lokasRepository, passwordEncoder);
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
}
