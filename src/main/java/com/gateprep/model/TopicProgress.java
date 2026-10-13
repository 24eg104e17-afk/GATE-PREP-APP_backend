package com.gateprep.model;

import jakarta.persistence.*;

/** A syllabus topic that a user has ticked as finished. */
@Entity
@Table(name = "topic_progress", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "topic_id"}))
public class TopicProgress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(name = "user_id", nullable = false)
    public Long userId;
    @Column(name = "topic_id", nullable = false)
    public Long topicId;
}
