package my.gov.jans.access.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import my.gov.jans.access.domain.TetapanSistem;
import my.gov.jans.access.repo.TetapanSistemRepository;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TetapanSistemServiceTest {
    private final TetapanSistemRepository repository = mock(TetapanSistemRepository.class);
    private final TetapanSistemService service = new TetapanSistemService(repository, new ObjectMapper());

    @Test
    void savesCustomAndLegacyStatusColors() throws Exception {
        TetapanSistem setting = statusSetting();
        when(repository.findById(1L)).thenReturn(Optional.of(setting));
        when(repository.save(any(TetapanSistem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.update("status", 1L, Map.of("key", "DIHANTAR", "value", Map.of("color", "#a14f90")));
        assertEquals("#a14f90", new ObjectMapper().readTree(setting.getSettingValue()).get("color").asText());
        service.update("status", 1L, Map.of("key", "DIHANTAR", "value", Map.of("color", "primary")));
        assertEquals("primary", new ObjectMapper().readTree(setting.getSettingValue()).get("color").asText());
        service.update("status", 1L, Map.of("key", "DIHANTAR", "value",
                Map.of("color", "secondary", "reviewedColor", "#1122aa")));
        assertEquals("#1122aa", new ObjectMapper().readTree(setting.getSettingValue()).get("reviewedColor").asText());
    }

    @Test
    void rejectsChangingStatusCodeOrInvalidColor() {
        TetapanSistem setting = statusSetting();
        when(repository.findById(1L)).thenReturn(Optional.of(setting));
        assertThrows(IllegalArgumentException.class, () -> service.update("status", 1L,
                Map.of("key", "LAIN", "value", Map.of("color", "#abcdef"))));
        assertThrows(IllegalArgumentException.class, () -> service.update("status", 1L,
                Map.of("key", "DIHANTAR", "value", Map.of("color", "red;display:none"))));
        assertThrows(IllegalArgumentException.class, () -> service.update("status", 1L,
                Map.of("key", "DIHANTAR", "value", Map.of("color", "secondary", "reviewedColor", "invalid"))));
        assertEquals("DIHANTAR", setting.getSettingKey());
    }

    @Test
    void allowsArbitraryUniqueAnnouncementKeys() {
        when(repository.saveAndFlush(any(TetapanSistem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        Map<String, Object> result = service.create("announcement",
                Map.of("key", "notis-air", "value", Map.of("title", "Notis")));
        assertEquals("notis-air", result.get("key"));
        verify(repository).saveAndFlush(any(TetapanSistem.class));
    }

    private TetapanSistem statusSetting() {
        TetapanSistem setting = new TetapanSistem();
        setting.setId(1L);
        setting.setCategory("status");
        setting.setSettingKey("DIHANTAR");
        setting.setSettingValue("{\"color\":\"secondary\"}");
        return setting;
    }
}
