package com.gateprep.web;

import com.gateprep.service.TestService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final TestService service;

    public ApiController(TestService service) {
        this.service = service;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    @GetMapping("/subjects")
    public List<Map<String, Object>> subjects() {
        return service.subjectsWithTopics();
    }

    @GetMapping("/questions")
    public List<Map<String, Object>> questions(@RequestParam(name = "subject_id", required = false) Long subjectId,
                                               @RequestParam(name = "topic_id", required = false) Long topicId) {
        return service.listQuestions(subjectId, topicId);
    }

    @PostMapping("/tests/start")
    public ResponseEntity<Map<String, Object>> start(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                                     @RequestBody(required = false) StartRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.start(userId, body));
    }

    @PostMapping("/tests/{id}/submit")
    public Map<String, Object> submit(@RequestAttribute(AuthInterceptor.USER_ID) Long userId, @PathVariable Long id,
                                      @RequestBody(required = false) SubmitRequest body) {
        return service.submit(userId, id, body);
    }

    @GetMapping("/attempts")
    public List<Map<String, Object>> attempts(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        return service.history(userId);
    }

    @GetMapping("/attempts/{id}")
    public Map<String, Object> attempt(@RequestAttribute(AuthInterceptor.USER_ID) Long userId, @PathVariable Long id) {
        return service.detail(userId, id);
    }
}
