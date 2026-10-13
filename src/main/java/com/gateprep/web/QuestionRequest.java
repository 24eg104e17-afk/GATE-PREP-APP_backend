package com.gateprep.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * A question to create or update.
 * type: MCQ, MSQ or NAT. marks: 1 or 2. options: 2 to 6 choices (leave empty for NAT).
 * answer: option letters such as "B" or "A,C" for MCQ/MSQ, a number for NAT. difficulty: easy, medium or hard.
 */
public record QuestionRequest(@JsonProperty("topic_id") Long topicId,
                              String type,
                              Integer marks,
                              String text,
                              List<String> options,
                              String answer,
                              String solution,
                              String difficulty) {
}
