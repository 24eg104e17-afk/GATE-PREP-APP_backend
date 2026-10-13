package com.gateprep.web;

import com.gateprep.model.TopicProgress;
import com.gateprep.repo.TopicProgressRepository;
import com.gateprep.repo.TopicRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Syllabus ticks, saved per user. */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {
    public record ProgressRequest(boolean done) {
    }

    private final TopicProgressRepository progress;
    private final TopicRepository topics;

    public ProgressController(TopicProgressRepository progress, TopicRepository topics) {
        this.progress = progress;
        this.topics = topics;
    }

    @GetMapping
    public List<Long> list(@RequestAttribute(AuthInterceptor.USER_ID) Long userId) {
        List<Long> ids = new ArrayList<>();
        for (TopicProgress p : progress.findByUserId(userId)) ids.add(p.topicId);
        return ids;
    }

    @PutMapping("/{topicId}")
    @Transactional
    public Map<String, Object> set(@RequestAttribute(AuthInterceptor.USER_ID) Long userId,
                                   @PathVariable Long topicId, @RequestBody ProgressRequest body) {
        if (!topics.existsById(topicId)) throw new ApiException(HttpStatus.NOT_FOUND, "Topic not found");
        Optional<TopicProgress> existing = progress.findByUserIdAndTopicId(userId, topicId);
        if (body.done() && existing.isEmpty()) {
            TopicProgress p = new TopicProgress();
            p.userId = userId;
            p.topicId = topicId;
            progress.save(p);
        } else if (!body.done() && existing.isPresent()) {
            progress.delete(existing.get());
        }
        return Map.of("topic_id", topicId, "done", body.done());
    }
}
