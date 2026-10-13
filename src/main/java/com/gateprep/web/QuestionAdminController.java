package com.gateprep.web;

import com.gateprep.service.QuestionService;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** CRUD for the question bank. Administrators only (see AdminInterceptor). */
@RestController
@RequestMapping("/api/admin/questions")
public class QuestionAdminController {
    private final QuestionService service;

    public QuestionAdminController(QuestionService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(name = "subject_id", required = false) Long subjectId,
                                          @RequestParam(name = "topic_id", required = false) Long topicId,
                                          @RequestParam(name = "search", required = false) String search) {
        return service.list(subjectId, topicId, search);
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody QuestionRequest body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(body));
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody QuestionRequest body) {
        return service.update(id, body);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        return service.delete(id);
    }
}
