package com.gateprep.model;

import jakarta.persistence.*;

@Entity
@Table(name = "attempt_questions")
public class AttemptQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "attempt_id")
    public Attempt attempt;
    @ManyToOne(optional = false) @JoinColumn(name = "question_id")
    public Question question;
    public int sortOrder;
    @Column(length = 500)
    public String response;
    public Double awarded;
}
