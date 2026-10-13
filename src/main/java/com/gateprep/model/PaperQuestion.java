package com.gateprep.model;

import jakarta.persistence.*;

@Entity
@Table(name = "paper_questions")
public class PaperQuestion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "paper_id")
    public Paper paper;
    @ManyToOne(optional = false) @JoinColumn(name = "question_id")
    public Question question;
    public int sortOrder;
}
