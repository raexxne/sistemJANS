package my.gov.jans.access.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import my.gov.jans.access.domain.TetapanSistem;
import my.gov.jans.access.repo.TetapanSistemRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class TetapanSistemService {
    public static final String PENGUMUMAN = "announcement";
    public static final String SMTP = "smtp";
    public static final String TEMPLATE = "email-template";
    public static final String STATUS = "status";

    private static final List<String> CATEGORIES = List.of(PENGUMUMAN, SMTP, TEMPLATE, STATUS);
    private static final List<String> STATUS_COLORS =
            List.of("primary", "secondary", "success", "danger", "warning", "info", "dark");

    private final TetapanSistemRepository repository;
    private final ObjectMapper objectMapper;

    public TetapanSistemService(TetapanSistemRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> list(String category) {
        String normalizedCategory = normalizeCategory(category);
        return repository.findAllByCategoryOrderByIdAsc(normalizedCategory).stream()
                .map(setting -> toResponse(setting, false))
                .toList();
    }

    public List<Map<String, Object>> listPublic(String category) {
        String normalizedCategory = normalizeCategory(category);
        return repository.findAllByCategoryOrderByIdAsc(normalizedCategory).stream()
                .map(setting -> toResponse(setting, true))
                .toList();
    }

    @Transactional
    public Map<String, Object> create(String category, Map<String, Object> payload) {
        String normalizedCategory = normalizeCategory(category);
        String key = normalizeKey(payload.get("key"));
        if (repository.existsByCategoryAndSettingKey(normalizedCategory, key)) {
            throw new IllegalArgumentException("Kunci tetapan sudah wujud");
        }
        TetapanSistem setting = new TetapanSistem();
        setting.setCategory(normalizedCategory);
        setting.setSettingKey(key);
        Map<String, Object> value = parseObject(payload.get("value"));
        validateStatusColor(normalizedCategory, value);
        setting.setSettingValue(serialize(value));
        try {
            return toResponse(repository.saveAndFlush(setting), false);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Kunci tetapan sudah wujud", e);
        }
    }

    @Transactional
    public Map<String, Object> update(String category, Long id, Map<String, Object> payload) {
        String normalizedCategory = normalizeCategory(category);
        TetapanSistem setting = repository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Tetapan tidak ditemui"));
        if (!normalizedCategory.equals(setting.getCategory())) {
            throw new IllegalArgumentException("Kategori tetapan tidak sepadan");
        }

        String key = normalizeKey(payload.get("key"));
        if (STATUS.equals(normalizedCategory) && !setting.getSettingKey().equals(key)) {
            throw new IllegalArgumentException("Kod status sistem tidak boleh diubah");
        }
        repository.findByCategoryAndSettingKey(normalizedCategory, key)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Kunci tetapan sudah digunakan");
                });

        Map<String, Object> value = parseObject(payload.get("value"));
        validateStatusColor(normalizedCategory, value);
        if (SMTP.equals(normalizedCategory)) {
            String password = String.valueOf(value.getOrDefault("password", ""));
            if (password.isBlank() || "********".equals(password)) {
                Map<String, Object> previous = read(setting);
                value.put("password", previous.getOrDefault("password", ""));
            }
        }
        setting.setSettingKey(key);
        setting.setSettingValue(serialize(value));
        return toResponse(repository.save(setting), false);
    }

    @Transactional
    public void delete(String category, Long id) {
        String normalizedCategory = normalizeCategory(category);
        TetapanSistem setting = repository.findById(Objects.requireNonNull(id))
                .orElseThrow(() -> new IllegalArgumentException("Tetapan tidak ditemui"));
        if (!normalizedCategory.equals(setting.getCategory())) {
            throw new IllegalArgumentException("Kategori tetapan tidak sepadan");
        }
        repository.delete(setting);
    }

    public Map<String, Object> find(String category, String key) {
        return repository.findByCategoryAndSettingKey(normalizeCategory(category), normalizeKey(key))
                .map(this::read)
                .orElse(Map.of());
    }

    private Map<String, Object> toResponse(TetapanSistem setting, boolean publicResponse) {
        Map<String, Object> response = new LinkedHashMap<>();
        Map<String, Object> value = read(setting);
        if (SMTP.equals(setting.getCategory()) && (publicResponse || value.containsKey("password"))) {
            value.put("password", "********");
        }
        response.put("id", setting.getId());
        response.put("category", setting.getCategory());
        response.put("key", setting.getSettingKey());
        response.put("value", value);
        response.put("updatedAt", setting.getUpdatedAt());
        return response;
    }

    private Map<String, Object> read(TetapanSistem setting) {
        try {
            return objectMapper.readValue(setting.getSettingValue(), new TypeReference<>() {});
        } catch (Exception e) {
            throw new IllegalStateException("Data tetapan " + setting.getSettingKey() + " tidak sah", e);
        }
    }

    private Map<String, Object> parseObject(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            map.forEach((key, item) -> converted.put(String.valueOf(key), item));
            return converted;
        }

        throw new IllegalArgumentException("Nilai tetapan mesti berbentuk objek");
    }

    private void validateStatusColor(String category, Map<String, Object> value) {
        if (!STATUS.equals(category)) return;
        for (String field : List.of("color", "reviewedColor")) {
            if ("reviewedColor".equals(field) && !value.containsKey(field)) continue;
            Object color = value.get(field);
            if (!(color instanceof String text)
                    || !(STATUS_COLORS.contains(text) || text.matches("#[0-9a-fA-F]{6}"))) {
                throw new IllegalArgumentException("Warna status mesti berupa kod warna hex atau warna Bootstrap yang disokong");
            }
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(parseObject(value));
        } catch (Exception e) {
            throw new IllegalArgumentException("Nilai tetapan tidak sah", e);
        }
    }

    private String normalizeCategory(String category) {
        String normalized = Objects.requireNonNull(category, "Kategori diperlukan").trim().toLowerCase(Locale.ROOT);
        if (!CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("Kategori tetapan tidak sah");
        }
        return normalized;
    }

    private String normalizeKey(Object key) {
        String normalized = Objects.toString(key, "").trim();
        if (normalized.isBlank() || !normalized.matches("[A-Za-z0-9_.-]+")) {
            throw new IllegalArgumentException("Kunci tetapan tidak sah");
        }
        return normalized;
    }
}
