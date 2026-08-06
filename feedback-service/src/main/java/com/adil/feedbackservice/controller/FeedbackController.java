package com.adil.feedbackservice.controller;

import com.adil.feedbackservice.dto.FeedbackRequest;
import com.adil.feedbackservice.model.Feedback;
import com.adil.feedbackservice.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public ResponseEntity<Feedback> createFeedback(
            @Valid @RequestBody FeedbackRequest request
    ) {
        Feedback createdFeedback =
                feedbackService.createFeedback(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdFeedback);
    }

    @GetMapping
    public ResponseEntity<List<Feedback>> getAllFeedback() {
        return ResponseEntity.ok(
                feedbackService.getAllFeedback()
        );
    }
}