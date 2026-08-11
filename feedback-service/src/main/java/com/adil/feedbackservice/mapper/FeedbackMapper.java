package com.adil.feedbackservice.mapper;

import com.adil.feedbackservice.dto.FeedbackResponse;
import com.adil.feedbackservice.model.Feedback;
import org.springframework.stereotype.Component;

@Component
public class FeedbackMapper {

    public FeedbackResponse toResponse(Feedback feedback) {
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getName(),
                feedback.getEmail(),
                feedback.getMessage(),
                feedback.getVersion(),
                feedback.getCreatedAt()
        );
    }
}