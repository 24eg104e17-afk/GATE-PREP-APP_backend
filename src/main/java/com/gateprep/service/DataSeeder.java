package com.gateprep.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gateprep.model.Question;
import com.gateprep.model.Subject;
import com.gateprep.model.Topic;
import com.gateprep.repo.QuestionRepository;
import com.gateprep.repo.SubjectRepository;
import com.gateprep.repo.TopicRepository;
import com.gateprep.web.QuestionRequest;
import java.io.InputStream;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** At startup: loads the GATE 2027 EC syllabus, and the starting questions from data/questions.json if the bank is empty. Safe to run again. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final SubjectRepository subjects;
    private final TopicRepository topics;
    private final QuestionRepository questions;
    private final ObjectMapper mapper;
    private final QuestionService questionService;

    public DataSeeder(SubjectRepository subjects, TopicRepository topics, QuestionRepository questions, ObjectMapper mapper,
                      QuestionService questionService) {
        this.subjects = subjects;
        this.topics = topics;
        this.questions = questions;
        this.mapper = mapper;
        this.questionService = questionService;
    }

    @Override
    public void run(String... args) throws Exception {
        seedSyllabus();
        importQuestions();
    }

    private void seedSyllabus() {
        int subjectOrder = 0;
        for (Map.Entry<String, List<String>> e : SyllabusData.SYLLABUS.entrySet()) {
            final int so = subjectOrder++;
            final String subjectName = e.getKey();
            Subject subject = subjects.findByName(subjectName).orElseGet(() -> {
                Subject s = new Subject();
                s.name = subjectName;
                s.sortOrder = so;
                return subjects.save(s);
            });
            int topicOrder = 0;
            for (String name : e.getValue()) {
                int to = topicOrder++;
                if (topics.findBySubjectIdAndName(subject.id, name).isEmpty()) {
                    Topic t = new Topic();
                    t.subject = subject;
                    t.name = name;
                    t.sortOrder = to;
                    topics.save(t);
                }
            }
        }
    }

    /** The starting questions load only into an empty bank. After that the bank is managed from the Question bank screen. */
    private void importQuestions() throws Exception {
        if (questions.count() > 0) return;
        try (InputStream in = getClass().getResourceAsStream("/data/questions.json")) {
            if (in == null) return;
            List<QuestionSeed> seeds = mapper.readValue(in, new TypeReference<List<QuestionSeed>>() { });
            int added = 0, skipped = 0;
            for (QuestionSeed seed : seeds) {
                Topic topic = topics.findBySubjectNameAndName(seed.subject(), seed.topic()).orElse(null);
                if (topic == null) {
                    System.out.println("Question skipped (unknown subject or topic): " + seed.text());
                    skipped++;
                    continue;
                }
                try {
                    Question q = new Question();
                    questionService.fill(q, topic, new QuestionRequest(topic.id, seed.type(), seed.marks(), seed.text(),
                            seed.options(), seed.answer(), seed.solution(), seed.difficulty()));
                    questions.save(q);
                    added++;
                } catch (IllegalArgumentException ex) {
                    System.out.println("Question skipped (" + ex.getMessage() + "): " + seed.text());
                    skipped++;
                }
            }
            System.out.println("Questions imported: " + added + " new, " + skipped + " skipped");
        }
    }
}
