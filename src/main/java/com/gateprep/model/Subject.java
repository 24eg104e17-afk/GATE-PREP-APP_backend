package com.gateprep.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, unique = true)
    public String name;
    public int sortOrder;
}
