package com.adil.feedbackservice.service;

import com.adil.feedbackservice.dto.FeedbackRequest;
import com.adil.feedbackservice.model.Feedback;
import com.adil.feedbackservice.repository.FeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    private FeedbackService feedbackService;

    @BeforeEach
    void setUp() {
        feedbackService = new FeedbackService(feedbackRepository);
    }

    @Test
    void createFeedback_shouldSaveNormalizedFeedback() {
        FeedbackRequest request = new FeedbackRequest(
                "  Test User  ",
                "  TEST@EXAMPLE.COM  ",
                "  Great service!  "
        );

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Feedback result =
                feedbackService.createFeedback(request);

        assertEquals("Test User", result.getName());
        assertEquals("test@example.com", result.getEmail());
        assertEquals("Great service!", result.getMessage());

        verify(feedbackRepository)
                .save(any(Feedback.class));
    }

    @Test
    void getAllFeedback_shouldReturnPage() {
        PageRequest pageable = PageRequest.of(0, 20);

        Feedback feedback1 = new Feedback(
                "User One",
                "one@example.com",
                "First feedback"
        );

        Feedback feedback2 = new Feedback(
                "User Two",
                "two@example.com",
                "Second feedback"
        );

        Page<Feedback> expectedPage = new PageImpl<>(
                List.of(feedback1, feedback2),
                pageable,
                2
        );

        when(feedbackRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Feedback> result =
                feedbackService.getAllFeedback(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(feedbackRepository)
                .findAll(pageable);
    }
}