package com.adil.feedbackservice.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "feedback")
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 320)
    private String email;

    @Column(nullable = false, length = 2000)
    private String message;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected Feedback() {
    }

    public Feedback(
            String name,
            String email,
            String message
    ) {
        this.name = name;
        this.email = email;
        this.message = message;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}