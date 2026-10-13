package com.gateprep.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 60)
    public String name;
    @Column(nullable = false, unique = true, length = 120)
    public String email;
    /** BCrypt hash. The plain password is never stored. */
    @Column(nullable = false, length = 100)
    public String passwordHash;
    @Column(name = "user_role", nullable = false, length = 10)
    public String role = "USER";
    public Instant createdAt;
}
