package my.gov.jans.access.config;

import my.gov.jans.access.repo.PenggunaRepository;
import my.gov.jans.access.repo.LokasRepository;
import my.gov.jans.access.repo.PermohonanRepository;
import my.gov.jans.access.repo.PenyeliaLojiRepository;
import my.gov.jans.access.service.AkaunService;
import my.gov.jans.access.service.PermohonanService;
import my.gov.jans.access.web.ApiController;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(ApiController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PenggunaRepository penggunaRepository;
    @MockitoBean
    private LokasRepository lokasRepository;
    @MockitoBean
    private PermohonanRepository permohonanRepository;
    @MockitoBean
    private PenyeliaLojiRepository penyeliaLojiRepository;
    @MockitoBean
    private AkaunService akaunService;
    @MockitoBean
    private PermohonanService permohonanService;

    @Test
    void requiresCsrfTokenForPublicPostRequests() throws Exception {
        mockMvc.perform(post("/api/public/forgot-password/request-code")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isForbidden());

        MvcResult tokenResponse = mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isOk())
                .andReturn();
        String token = tokenResponse.getResponse().getContentAsString()
                .replaceAll("^.*\"token\":\"([^\"]+)\".*$", "$1");
        Cookie csrfCookie = tokenResponse.getResponse().getCookie("XSRF-TOKEN");

        assertNotNull(csrfCookie);
        mockMvc.perform(post("/api/public/forgot-password/request-code")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/public/permohonan")
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", token)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ralat").isNotEmpty())
                .andExpect(jsonPath("$.ralatMedan.emailWakil").isNotEmpty());
    }
}
