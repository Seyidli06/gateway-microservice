package com.adil.feedbackservice.service;

import com.adil.feedbackservice.dto.FeedbackRequest;
import com.adil.feedbackservice.model.Feedback;
import com.adil.feedbackservice.repository.FeedbackRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    @Transactional
    public Feedback createFeedback(FeedbackRequest request) {
        Feedback feedback = new Feedback(
                request.getName().trim(),
                normalizeEmail(request.getEmail()),
                request.getMessage().trim()
        );

        return feedbackRepository.save(feedback);
    }

    public Page<Feedback> getAllFeedback(Pageable pageable) {
        return feedbackRepository.findAll(pageable);
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}