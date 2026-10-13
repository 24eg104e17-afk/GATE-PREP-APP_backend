package com.gateprep.web;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

/** responses: question id -> list of option indexes (MCQ/MSQ) or a number (NAT). */
public record SubmitRequest(Map<String, JsonNode> responses) {
}
