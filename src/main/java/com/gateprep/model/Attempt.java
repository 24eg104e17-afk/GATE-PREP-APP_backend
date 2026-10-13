package com.gateprep.model;

import jakarta.persistence.*;
import java.time.Instant;

/** One mock test. userId stays empty until login is added. */
@Entity
@Table(name = "attempts")
public class Attempt {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long userId;
    public Instant startedAt;
    public int durationSec;
    public Instant submittedAt;
    public Double score;
    public Integer totalMarks;
}
