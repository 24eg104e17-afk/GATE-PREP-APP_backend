package com.gateprep.web;

import com.gateprep.service.PaperService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** The paper builder. Every endpoint needs a login (see WebConfig). */
@RestController
@RequestMapping("/api/papers")
public class PaperController {
    private final PaperService service;

    public PaperController(PaperService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> build(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                                     @RequestBody(required = false) BuildPaperRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.build(userId, body));
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return service.list(userId);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<Map<String, Object>> start(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                                     @PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.startPaper(userId, id));
    }

    public record RenameRequest(String name) {
    }

    @PutMapping("/{id}")
    public Map<String, Object> rename(@RequestAttribute(AuthInterceptor.USER_ID) Long userId, @PathVariable Long id,
                                      @RequestBody RenameRequest body) {
        return service.rename(userId, id, body.name());
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@RequestAttribute(AuthInterceptor.USER_ID) Long userId, @PathVariable Long id) {
        service.delete(userId, id);
        return Map.of("ok", true);
    }
}
