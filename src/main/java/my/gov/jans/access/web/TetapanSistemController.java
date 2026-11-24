package my.gov.jans.access.web;

import my.gov.jans.access.service.TetapanSistemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TetapanSistemController {
    private final TetapanSistemService service;

    public TetapanSistemController(TetapanSistemService service) {
        this.service = service;
    }

    @GetMapping("/public/settings/announcements")
    ResponseEntity<?> pengumumanAktif() {
        return ResponseEntity.ok(service.listPublic(TetapanSistemService.PENGUMUMAN).stream()
                .filter(item -> Boolean.TRUE.equals(((Map<?, ?>) item.get("value")).get("active")))
                .sorted(Comparator.comparingInt(item -> asInt(((Map<?, ?>) item.get("value")).get("sortOrder"))))
                .toList());
    }

    @GetMapping("/public/settings/statuses")
    ResponseEntity<?> konfigurasiStatus() {
        return ResponseEntity.ok(service.listPublic(TetapanSistemService.STATUS));
    }

    @GetMapping("/admin/settings/{category}")
    ResponseEntity<?> senarai(@PathVariable String category) {
        try {
            return ResponseEntity.ok(service.list(category));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    @PostMapping("/admin/settings/{category}")
    ResponseEntity<?> cipta(@PathVariable String category, @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(service.create(category, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    @PutMapping("/admin/settings/{category}/{id}")
    ResponseEntity<?> kemasKini(@PathVariable String category, @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            return ResponseEntity.ok(service.update(category, id, body));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    @DeleteMapping("/admin/settings/{category}/{id}")
    ResponseEntity<?> padam(@PathVariable String category, @PathVariable Long id) {
        try {
            service.delete(category, id);
            return ResponseEntity.ok(Map.of("berjaya", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("ralat", e.getMessage()));
        }
    }

    private int asInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
}
