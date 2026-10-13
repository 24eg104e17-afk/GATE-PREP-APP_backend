package com.gateprep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateprep.model.*;
import com.gateprep.repo.*;
import com.gateprep.web.ApiException;
import com.gateprep.web.StartRequest;
import com.gateprep.web.SubmitRequest;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestService {
    private static final int GRACE_SEC = 30;
    private static final int SEC_PER_QUESTION = 165; // about GATE pace (180 min for 65 questions)

    private final SubjectRepository subjects;
    private final TopicRepository topics;
    private final QuestionRepository questions;
    private final AttemptRepository attempts;
    private final AttemptQuestionRepository attemptQuestions;
    private final ObjectMapper mapper;

    public TestService(SubjectRepository subjects, TopicRepository topics, QuestionRepository questions,
                       AttemptRepository attempts, AttemptQuestionRepository attemptQuestions, ObjectMapper mapper) {
        this.subjects = subjects;
        this.topics = topics;
        this.questions = questions;
        this.attempts = attempts;
        this.attemptQuestions = attemptQuestions;
        this.mapper = mapper;
    }

    private JsonNode json(String s) {
        try {
            return s == null ? null : mapper.readTree(s);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A question as the test taker sees it: no answer and no solution. */
    private Map<String, Object> publicQuestion(Question q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.id);
        m.put("subject", q.topic.subject.name);
        m.put("topic", q.topic.name);
        m.put("type", q.qtype);
        m.put("marks", q.marks);
        m.put("text", q.text);
        m.put("options", json(q.options));
        return m;
    }

    public List<Map<String, Object>> subjectsWithTopics() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Subject s : subjects.findAllByOrderBySortOrderAsc()) {
            List<Map<String, Object>> ts = new ArrayList<>();
            for (Topic t : topics.findBySubjectIdOrderBySortOrderAsc(s.id)) {
                Map<String, Object> tm = new LinkedHashMap<>();
                tm.put("id", t.id);
                tm.put("name", t.name);
                tm.put("questions", questions.countByTopicIdAndActiveTrue(t.id));
                ts.add(tm);
            }
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", s.id);
            sm.put("name", s.name);
            sm.put("topics", ts);
            out.add(sm);
        }
        return out;
    }

    public List<Map<String, Object>> listQuestions(Long subjectId, Long topicId) {
        List<Question> list = topicId != null ? questions.findByTopicIdAndActiveTrue(topicId)
                : subjectId != null ? questions.findByTopicSubjectIdAndActiveTrue(subjectId) : questions.findByActiveTrue();
        List<Question> sorted = new ArrayList<>(list);
        sorted.sort(Comparator.comparing(q -> q.id));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Question q : sorted) out.add(publicQuestion(q));
        return out;
    }

    @Transactional
    public Map<String, Object> start(Long userId, StartRequest body) {
        int count = body == null || body.count() == null ? 10 : Math.max(1, Math.min(body.count(), 65));
        Long subjectId = body == null ? null : body.subjectId();
        List<Question> pool = new ArrayList<>(subjectId == null ? questions.findByActiveTrue() : questions.findByTopicSubjectIdAndActiveTrue(subjectId));
        if (pool.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "No questions available for this selection");
        Collections.shuffle(pool);
        List<Question> picked = new ArrayList<>(pool.subList(0, Math.min(count, pool.size())));
        int duration = body != null && body.durationMin() != null ? body.durationMin() * 60 : picked.size() * SEC_PER_QUESTION;
        return startFromQuestions(userId, picked, duration);
    }

    /** Creates a test (attempt) for exactly these questions, in this order. Used by quick tests and by saved papers. */
    @Transactional
    public Map<String, Object> startFromQuestions(Long userId, List<Question> picked, int duration) {
        int total = 0;
        for (Question q : picked) total += q.marks;

        Attempt a = new Attempt();
        a.userId = userId;
        a.startedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        a.durationSec = duration;
        a.totalMarks = total;
        attempts.save(a);
        for (int i = 0; i < picked.size(); i++) {
            AttemptQuestion aq = new AttemptQuestion();
            aq.attempt = a;
            aq.question = picked.get(i);
            aq.sortOrder = i;
            attemptQuestions.save(aq);
        }
        List<Map<String, Object>> qs = new ArrayList<>();
        for (Question q : picked) qs.add(publicQuestion(q));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attempt_id", a.id);
        out.put("duration_sec", duration);
        out.put("total_marks", total);
        out.put("questions", qs);
        return out;
    }

    @Transactional
    public Map<String, Object> submit(Long userId, Long id, SubmitRequest body) {
        Attempt a = attempts.findByIdAndUserId(id, userId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Test not found"));
        if (a.submittedAt != null) throw new ApiException(HttpStatus.CONFLICT, "This test was already submitted");
        Map<String, JsonNode> responses = body == null || body.responses() == null ? Map.of() : body.responses();
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        double score = 0;
        for (AttemptQuestion aq : attemptQuestions.findByAttemptIdOrderBySortOrder(id)) {
            Question q = aq.question;
            JsonNode resp = responses.get(String.valueOf(q.id));
            double awarded = Math.round(Grading.grade(q.qtype, q.marks, json(q.answer), resp) * 10000) / 10000.0;
            score += awarded;
            aq.response = resp == null || resp.isNull() ? null : resp.toString();
            aq.awarded = awarded;
            attemptQuestions.save(aq);
        }
        score = Math.round(score * 100) / 100.0;
        a.submittedAt = now;
        a.score = score;
        attempts.save(a);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attempt_id", a.id);
        out.put("score", score);
        out.put("total_marks", a.totalMarks);
        out.put("late", Duration.between(a.startedAt, now).getSeconds() > a.durationSec + GRACE_SEC);
        out.put("results", review(a.id));
        return out;
    }

    public List<Map<String, Object>> history(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Attempt a : attempts.findByUserIdAndSubmittedAtIsNotNullOrderByIdDesc(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id);
            m.put("started_at", a.startedAt.toString());
            m.put("submitted_at", a.submittedAt.toString());
            m.put("score", a.score);
            m.put("total_marks", a.totalMarks);
            out.add(m);
        }
        return out;
    }

    public Map<String, Object> detail(Long userId, Long id) {
        Attempt a = attempts.findByIdAndUserIdAndSubmittedAtIsNotNull(id, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Submitted test not found"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("attempt_id", a.id);
        out.put("score", a.score);
        out.put("total_marks", a.totalMarks);
        out.put("results", review(a.id));
        return out;
    }

    private List<Map<String, Object>> review(Long attemptId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (AttemptQuestion aq : attemptQuestions.findByAttemptIdOrderBySortOrder(attemptId)) {
            Map<String, Object> m = publicQuestion(aq.question);
            m.put("response", json(aq.response));
            m.put("correct_answer", json(aq.question.answer));
            m.put("awarded", aq.awarded);
            m.put("solution", aq.question.solution == null ? "" : aq.question.solution);
            out.add(m);
        }
        return out;
    }
}
