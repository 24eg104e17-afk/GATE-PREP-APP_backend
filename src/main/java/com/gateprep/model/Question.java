package com.gateprep.model;

import jakarta.persistence.*;

/** options: JSON list of strings (MCQ/MSQ). answer: JSON list of option indexes (MCQ/MSQ) or a number (NAT). */
@Entity
@Table(name = "questions")
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "topic_id")
    public Topic topic;
    @Column(nullable = false, length = 3)
    public String qtype;
    public int marks;
    @Column(name = "qtext", nullable = false, length = 2000)
    public String text;
    @Column(name = "options_json", length = 2000)
    public String options;
    @Column(nullable = false, length = 200)
    public String answer;
    @Column(length = 4000)
    public String solution;
    @Column(length = 10)
    public String difficulty = "medium";
    /** false = archived. Hidden from new tests and papers, but past test reviews still show it. */
    @Column(nullable = false)
    public boolean active = true;
}
