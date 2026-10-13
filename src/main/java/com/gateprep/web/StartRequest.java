package com.gateprep.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StartRequest(Integer count,
                           @JsonProperty("subject_id") Long subjectId,
                           @JsonProperty("duration_min") Integer durationMin) {
}
