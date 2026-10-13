package com.gateprep.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the exam information page (dates, fees, pattern, exam-day rules, links) from data/exam-info.json.
 * To update a date, edit that file and restart the server. This endpoint is public.
 */
@RestController
@RequestMapping("/api")
public class ExamInfoController {
    private final JsonNode info;

    public ExamInfoController(ObjectMapper mapper) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/data/exam-info.json")) {
            if (in == null) throw new IllegalStateException("data/exam-info.json is missing");
            this.info = mapper.readTree(in);
        }
    }

    @GetMapping("/exam-info")
    public JsonNode examInfo() {
        return info;
    }
}
