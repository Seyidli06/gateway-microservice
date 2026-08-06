package com.adil.feedbackservice.service;

import com.adil.feedbackservice.dto.FeedbackRequest;
import com.adil.feedbackservice.model.Feedback;
import com.adil.feedbackservice.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback createFeedback(FeedbackRequest request) {
        Feedback feedback = new Feedback(
                null,
                request.getName(),
                request.getEmail(),
                request.getMessage(),
                LocalDateTime.now()
        );

        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }
}