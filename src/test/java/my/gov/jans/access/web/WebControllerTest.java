package my.gov.jans.access.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WebControllerTest {

    @Test
    void shouldRedirectAdminToAdminPage() {
        WebController controller = new WebController();
        TestingAuthenticationToken auth = new TestingAuthenticationToken(
                "admin",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertEquals("redirect:/admin.html", controller.utama(auth));
    }
}
