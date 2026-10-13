package com.gateprep.service;

import java.util.List;

/**
 * One entry of data/questions.json.
 * type: MCQ, MSQ or NAT. answer: option letters like "B" or "A,B,D" for MCQ/MSQ, a number for NAT.
 * difficulty: easy, medium or hard (optional).
 */
public record QuestionSeed(String subject, String topic, String type, int marks, String text,
                           List<String> options, String answer, String solution, String difficulty) {
}
