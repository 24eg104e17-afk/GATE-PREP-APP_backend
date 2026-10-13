package com.gateprep.model;

import jakarta.persistence.*;

@Entity
@Table(name = "topics", uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "name"}))
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "subject_id")
    public Subject subject;
    @Column(nullable = false, length = 500)
    public String name;
    public int sortOrder;
}
