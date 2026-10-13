package com.gateprep.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateprep.model.Question;
import com.gateprep.model.Topic;
import com.gateprep.repo.AttemptQuestionRepository;
import com.gateprep.repo.PaperQuestionRepository;
import com.gateprep.repo.QuestionRepository;
import com.gateprep.repo.TopicRepository;
import com.gateprep.web.ApiException;
import com.gateprep.web.QuestionRequest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Create, read, update and delete for the question bank. */
@Service
public class QuestionService {
    private final QuestionRepository questions;
    private final TopicRepository topics;
    private final AttemptQuestionRepository attemptQuestions;
    private final PaperQuestionRepository paperQuestions;
    private final ObjectMapper mapper;

    public QuestionService(QuestionRepository questions, TopicRepository topics, AttemptQuestionRepository attemptQuestions,
                           PaperQuestionRepository paperQuestions, ObjectMapper mapper) {
        this.questions = questions;
        this.topics = topics;
        this.attemptQuestions = attemptQuestions;
        this.paperQuestions = paperQuestions;
        this.mapper = mapper;
    }

    private JsonNode json(String s) {
        try {
            return s == null ? null : mapper.readTree(s);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** The stored answer as text: "A,C" for MCQ/MSQ, "10" for NAT. */
    private String answerText(Question q) {
        JsonNode a = json(q.answer);
        if (q.qtype.equals("NAT")) {
            double d = a.asDouble();
            return d == Math.rint(d) ? String.valueOf((long) d) : String.valueOf(d);
        }
        List<String> letters = new ArrayList<>();
        for (JsonNode n : a) letters.add(String.valueOf("ABCDEF".charAt(n.asInt())));
        return String.join(",", letters);
    }

    /** A question with its answer and solution. Only for administrators. */
    public Map<String, Object> view(Question q) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", q.id);
        m.put("subject_id", q.topic.subject.id);
        m.put("subject", q.topic.subject.name);
        m.put("topic_id", q.topic.id);
        m.put("topic", q.topic.name);
        m.put("type", q.qtype);
        m.put("marks", q.marks);
        m.put("text", q.text);
        m.put("options", json(q.options));
        m.put("answer", answerText(q));
        m.put("solution", q.solution == null ? "" : q.solution);
        m.put("difficulty", q.difficulty);
        return m;
    }

    private Question active(Long id) {
        return questions.findById(id).filter(q -> q.active)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Question not found"));
    }

    private Topic topicOf(QuestionRequest r) {
        if (r.topicId() == null) throw new ApiException(HttpStatus.BAD_REQUEST, "Choose a topic");
        return topics.findById(r.topicId()).orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Topic not found"));
    }

    /** Checks the request and copies it into the question. Throws IllegalArgumentException with a plain message. */
    public void fill(Question q, Topic topic, QuestionRequest r) {
        String type = r.type() == null ? "" : r.type().trim().toUpperCase(Locale.ROOT);
        if (!List.of("MCQ", "MSQ", "NAT").contains(type)) throw new IllegalArgumentException("Type must be MCQ, MSQ or NAT");
        if (r.marks() == null || (r.marks() != 1 && r.marks() != 2)) throw new IllegalArgumentException("Marks must be 1 or 2");
        String text = r.text() == null ? "" : r.text().trim();
        if (text.isEmpty() || text.length() > 2000) throw new IllegalArgumentException("Enter the question text (up to 2000 characters)");
        String solution = r.solution() == null ? "" : r.solution().trim();
        if (solution.length() > 4000) throw new IllegalArgumentException("The solution is too long (up to 4000 characters)");
        String difficulty = r.difficulty() == null || r.difficulty().isBlank() ? "medium" : r.difficulty().trim().toLowerCase(Locale.ROOT);
        if (!List.of("easy", "medium", "hard").contains(difficulty)) throw new IllegalArgumentException("Difficulty must be easy, medium or hard");
        String answerIn = r.answer() == null ? "" : r.answer().trim();

        String answerJson;
        String optionsJson = null;
        try {
            if (type.equals("NAT")) {
                try {
                    answerJson = String.valueOf(Double.parseDouble(answerIn));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("The answer of a NAT question must be a number");
                }
            } else {
                List<String> opts = new ArrayList<>();
                if (r.options() != null) for (String o : r.options()) if (o != null && !o.isBlank()) opts.add(o.trim());
                if (opts.size() < 2 || opts.size() > 6) throw new IllegalArgumentException("Give 2 to 6 options");
                List<Integer> idx = new ArrayList<>();
                for (String part : answerIn.split(",")) {
                    if (part.isBlank()) continue;
                    int k = "ABCDEF".indexOf(part.trim().toUpperCase(Locale.ROOT));
                    if (k < 0 || k >= opts.size()) throw new IllegalArgumentException("The answer must use the letters of the options");
                    if (!idx.contains(k)) idx.add(k);
                }
                if (idx.isEmpty()) throw new IllegalArgumentException("Choose the correct answer");
                if (type.equals("MCQ") && idx.size() != 1) throw new IllegalArgumentException("An MCQ has exactly one correct option");
                Collections.sort(idx);
                answerJson = mapper.writeValueAsString(idx);
                optionsJson = mapper.writeValueAsString(opts);
                if (optionsJson.length() > 2000) throw new IllegalArgumentException("The options are too long");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
        q.topic = topic;
        q.qtype = type;
        q.marks = r.marks();
        q.text = text;
        q.options = optionsJson;
        q.answer = answerJson;
        q.solution = solution;
        q.difficulty = difficulty;
    }

    private void fillOrBadRequest(Question q, Topic topic, QuestionRequest r) {
        try {
            fill(q, topic, r);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // ---- Read ----

    public List<Map<String, Object>> list(Long subjectId, Long topicId, String search) {
        String needle = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        List<Question> all = new ArrayList<>(questions.findByActiveTrue());
        all.sort(Comparator.comparing((Question q) -> q.id).reversed());
        List<Map<String, Object>> out = new ArrayList<>();
        for (Question q : all) {
            if (topicId != null && !q.topic.id.equals(topicId)) continue;
            if (subjectId != null && !q.topic.subject.id.equals(subjectId)) continue;
            if (!needle.isEmpty() && !q.text.toLowerCase(Locale.ROOT).contains(needle)) continue;
            out.add(view(q));
        }
        return out;
    }

    public Map<String, Object> get(Long id) {
        return view(active(id));
    }

    // ---- Create ----

    @Transactional
    public Map<String, Object> create(QuestionRequest r) {
        Topic topic = topicOf(r);
        Question q = new Question();
        fillOrBadRequest(q, topic, r);
        if (questions.existsByTopicIdAndText(topic.id, q.text))
            throw new ApiException(HttpStatus.CONFLICT, "This question already exists in that topic");
        questions.save(q);
        return view(q);
    }

    // ---- Update ----

    @Transactional
    public Map<String, Object> update(Long id, QuestionRequest r) {
        Question q = active(id);
        Topic topic = topicOf(r);
        Question next = new Question(); // checked first, so the duplicate test cannot find this question itself
        fillOrBadRequest(next, topic, r);
        boolean moved = !topic.id.equals(q.topic.id) || !next.text.equals(q.text);
        if (moved && questions.existsByTopicIdAndText(topic.id, next.text))
            throw new ApiException(HttpStatus.CONFLICT, "This question already exists in that topic");
        q.topic = next.topic;
        q.qtype = next.qtype;
        q.marks = next.marks;
        q.text = next.text;
        q.options = next.options;
        q.answer = next.answer;
        q.solution = next.solution;
        q.difficulty = next.difficulty;
        questions.save(q);
        return view(q);
    }

    // ---- Delete ----

    /** Deletes the question. If a past test or a saved paper uses it, it is archived instead so those stay readable. */
    @Transactional
    public Map<String, Object> delete(Long id) {
        Question q = active(id);
        boolean inUse = attemptQuestions.existsByQuestionId(id) || paperQuestions.existsByQuestionId(id);
        if (inUse) {
            q.active = false;
            questions.save(q);
        } else {
            questions.delete(q);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("deleted", true);
        out.put("archived", inUse);
        return out;
    }
}
