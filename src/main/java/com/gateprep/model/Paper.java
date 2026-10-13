package com.gateprep.model;

import jakarta.persistence.*;
import java.time.Instant;

/** A saved question paper built by the paper builder. Its questions are in PaperQuestion. */
@Entity
@Table(name = "papers")
public class Paper {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(nullable = false, length = 120)
    public String name;
    public Instant createdAt;
    public int durationSec;
    public int totalMarks;
    /** False when the question bank could not fill the requested pattern. */
    public boolean fullyBuilt;
    @Column(length = 200)
    public String blueprint;
}
