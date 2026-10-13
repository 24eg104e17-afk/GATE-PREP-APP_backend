package com.gateprep.service;

import com.gateprep.model.Paper;
import com.gateprep.model.PaperQuestion;
import com.gateprep.model.Question;
import com.gateprep.model.Subject;
import com.gateprep.repo.PaperQuestionRepository;
import com.gateprep.repo.PaperRepository;
import com.gateprep.repo.QuestionRepository;
import com.gateprep.repo.SubjectRepository;
import com.gateprep.service.PaperBuilder.Blueprint;
import com.gateprep.service.PaperBuilder.Candidate;
import com.gateprep.web.ApiException;
import com.gateprep.web.BuildPaperRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Builds, saves, lists, starts and deletes question papers. The picking itself is in PaperBuilder. */
@Service
public class PaperService {
    private static final int FULL_PAPER_SEC = 180 * 60;
    private static final int MAX_QUESTIONS = 65;

    private final QuestionRepository questions;
    private final SubjectRepository subjects;
    private final PaperRepository papers;
    private final PaperQuestionRepository paperQuestions;
    private final TestService tests;

    public PaperService(QuestionRepository questions, SubjectRepository subjects, PaperRepository papers,
                        PaperQuestionRepository paperQuestions, TestService tests) {
        this.questions = questions;
        this.subjects = subjects;
        this.papers = papers;
        this.paperQuestions = paperQuestions;
        this.tests = tests;
    }

    private static int clamp(Integer v, int max) {
        return v == null ? 0 : Math.max(0, Math.min(v, max));
    }

    @Transactional
    public Map<String, Object> build(Long userId, BuildPaperRequest body) {
        boolean full = body == null || body.mode() == null || body.mode().equalsIgnoreCase("full");
        List<Long> subjectIds = full || body.subjectIds() == null ? List.of() : body.subjectIds();

        // Candidates: General Aptitude always, core questions limited to the chosen subjects (all if none chosen).
        Map<Long, Question> byId = new HashMap<>();
        List<Candidate> pool = new ArrayList<>();
        for (Question q : questions.findByActiveTrue()) {
            Subject s = q.topic.subject;
            boolean ga = s.name.equals(PaperBuilder.GA_SUBJECT);
            if (!ga && !subjectIds.isEmpty() && !subjectIds.contains(s.id)) continue;
            byId.put(q.id, q);
            pool.add(new Candidate(q.id, s.name, ga ? "GA" : "CORE", q.marks));
        }

        Blueprint bp;
        if (full) {
            bp = Blueprint.gate();
        } else {
            long mathId = subjects.findByName(PaperBuilder.MATH_SUBJECT).map(s -> s.id).orElse(-1L);
            boolean mathChosen = subjectIds.isEmpty() || subjectIds.contains(mathId);
            bp = new Blueprint(clamp(body.ga1(), MAX_QUESTIONS), clamp(body.ga2(), MAX_QUESTIONS),
                    clamp(body.core1(), MAX_QUESTIONS), clamp(body.core2(), MAX_QUESTIONS),
                    mathChosen ? clamp(body.mathMarks(), 30) : 0);
            if (bp.totalQuestions() < 1 || bp.totalQuestions() > MAX_QUESTIONS)
                throw new ApiException(HttpStatus.BAD_REQUEST, "A paper needs between 1 and " + MAX_QUESTIONS + " questions");
        }

        PaperBuilder.Result result = PaperBuilder.build(pool, bp, new Random());
        List<Candidate> picked = result.picked();
        if (picked.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "The question bank has no questions for this paper");

        int totalMarks = 0;
        long ga1 = 0, ga2 = 0, core1 = 0, core2 = 0;
        int mathMarks = 0;
        Map<String, Integer> bySubject = new LinkedHashMap<>();
        for (Candidate c : picked) {
            totalMarks += c.marks();
            if (c.section().equals("GA")) {
                if (c.marks() == 1) ga1++; else ga2++;
            } else {
                if (c.marks() == 1) core1++; else core2++;
                bySubject.merge(c.subject(), 1, Integer::sum);
                if (c.subject().equals(PaperBuilder.MATH_SUBJECT)) mathMarks += c.marks();
            }
        }
        int duration = full ? FULL_PAPER_SEC
                : body.durationMin() != null && body.durationMin() > 0 ? body.durationMin() * 60
                : (int) Math.ceil(picked.size() * 180.0 / 65) * 60;
        String defaultName = full ? "Full GATE paper" : "Custom paper";
        String name = body != null && body.name() != null && !body.name().isBlank() ? body.name().trim() : defaultName;
        if (name.length() > 120) name = name.substring(0, 120);

        Paper paper = new Paper();
        paper.userId = userId;
        paper.name = name;
        paper.createdAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        paper.durationSec = duration;
        paper.totalMarks = totalMarks;
        paper.fullyBuilt = result.fullyBuilt();
        paper.blueprint = "ga1=" + bp.ga1() + ",ga2=" + bp.ga2() + ",core1=" + bp.core1() + ",core2=" + bp.core2() + ",math=" + bp.mathMarks();
        papers.save(paper);
        for (int i = 0; i < picked.size(); i++) {
            PaperQuestion pq = new PaperQuestion();
            pq.paper = paper;
            pq.question = byId.get(picked.get(i).id());
            pq.sortOrder = i;
            paperQuestions.save(pq);
        }

        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("ga_1_mark", ga1);
        counts.put("ga_2_mark", ga2);
        counts.put("core_1_mark", core1);
        counts.put("core_2_mark", core2);
        counts.put("math_marks", mathMarks);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("paper_id", paper.id);
        out.put("name", paper.name);
        out.put("fully_built", paper.fullyBuilt);
        out.put("total_questions", picked.size());
        out.put("total_marks", totalMarks);
        out.put("duration_sec", duration);
        out.put("counts", counts);
        out.put("subjects", bySubject);
        out.put("shortfall", result.shortfall());
        return out;
    }

    public List<Map<String, Object>> list(Long userId) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Paper p : papers.findByUserIdOrderByIdDesc(userId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id);
            m.put("name", p.name);
            m.put("created_at", p.createdAt.toString());
            m.put("total_questions", paperQuestions.countByPaperId(p.id));
            m.put("total_marks", p.totalMarks);
            m.put("duration_sec", p.durationSec);
            m.put("fully_built", p.fullyBuilt);
            out.add(m);
        }
        return out;
    }

    /** Starts a test from a saved paper. The response has the same shape as /api/tests/start. */
    @Transactional
    public Map<String, Object> startPaper(Long userId, Long paperId) {
        Paper paper = papers.findByIdAndUserId(paperId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Paper not found"));
        List<Question> list = new ArrayList<>();
        for (PaperQuestion pq : paperQuestions.findByPaperIdOrderBySortOrder(paperId)) list.add(pq.question);
        return tests.startFromQuestions(userId, list, paper.durationSec);
    }

    @Transactional
    public Map<String, Object> rename(Long userId, Long paperId, String newName) {
        Paper paper = papers.findByIdAndUserId(paperId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Paper not found"));
        String name = newName == null ? "" : newName.trim();
        if (name.isEmpty() || name.length() > 120) throw new ApiException(HttpStatus.BAD_REQUEST, "Enter a name (up to 120 characters)");
        paper.name = name;
        papers.save(paper);
        return Map.of("id", paper.id, "name", paper.name);
    }

    @Transactional
    public void delete(Long userId, Long paperId) {
        Paper paper = papers.findByIdAndUserId(paperId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Paper not found"));
        paperQuestions.deleteByPaperId(paperId);
        papers.delete(paper);
    }
}
