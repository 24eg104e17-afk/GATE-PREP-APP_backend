package com.gateprep.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * mode "full" builds the real GATE pattern and ignores the other fields except name.
 * mode "custom" uses the counts: ga_1 and ga_2 (General Aptitude), core_1 and core_2 (subject questions),
 * math_marks (marks of Engineering Mathematics inside the core part, 0 for no rule),
 * subject_ids (limits the core questions to these subjects) and duration_min (default: real exam pace).
 */
public record BuildPaperRequest(String name,
                                String mode,
                                @JsonProperty("ga_1") Integer ga1,
                                @JsonProperty("ga_2") Integer ga2,
                                @JsonProperty("core_1") Integer core1,
                                @JsonProperty("core_2") Integer core2,
                                @JsonProperty("math_marks") Integer mathMarks,
                                @JsonProperty("subject_ids") List<Long> subjectIds,
                                @JsonProperty("duration_min") Integer durationMin) {
}
