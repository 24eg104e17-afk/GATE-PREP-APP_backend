package com.gateprep.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import java.util.TreeSet;

/** GATE marking: MCQ loses 1/3 (1-mark) or 2/3 (2-mark) for a wrong answer. MSQ needs every correct option. NAT has no negative marks. */
public final class Grading {
    private Grading() {
    }

    public static double grade(String qtype, int marks, JsonNode answer, JsonNode response) {
        if (response == null || response.isNull()) return 0;
        if (response.isTextual() && response.asText().isBlank()) return 0;
        if (response.isArray() && response.size() == 0) return 0;
        if (qtype.equals("NAT")) {
            try {
                double v = response.isNumber() ? response.asDouble() : Double.parseDouble(response.asText().trim());
                return Math.abs(v - answer.asDouble()) <= 0.01 ? marks : 0;
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (!response.isArray()) return 0;
        Set<Integer> chosen = new TreeSet<>();
        response.forEach(n -> chosen.add(n.asInt()));
        Set<Integer> correct = new TreeSet<>();
        answer.forEach(n -> correct.add(n.asInt()));
        if (chosen.equals(correct)) return marks;
        if (qtype.equals("MCQ")) return marks == 1 ? -1.0 / 3 : -2.0 / 3;
        return 0;
    }
}
