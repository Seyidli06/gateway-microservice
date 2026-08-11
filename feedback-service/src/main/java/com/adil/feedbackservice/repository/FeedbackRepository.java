package com.adil.feedbackservice.repository;

import com.adil.feedbackservice.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}