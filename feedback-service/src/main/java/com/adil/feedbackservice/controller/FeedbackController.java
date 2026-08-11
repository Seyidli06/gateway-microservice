package com.adil.feedbackservice.controller;

import com.adil.feedbackservice.dto.FeedbackRequest;
import com.adil.feedbackservice.dto.FeedbackResponse;
import com.adil.feedbackservice.dto.PageResponse;
import com.adil.feedbackservice.mapper.FeedbackMapper;
import com.adil.feedbackservice.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/feedback")
@Tag(
        name = "Feedback",
        description = "Operations for submitting and retrieving feedback"
)
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final FeedbackMapper feedbackMapper;

    public FeedbackController(
            FeedbackService feedbackService,
            FeedbackMapper feedbackMapper
    ) {
        this.feedbackService = feedbackService;
        this.feedbackMapper = feedbackMapper;
    }

    @PostMapping
    @Operation(
            summary = "Submit feedback",
            description = "Creates a new feedback entry"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Feedback created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            )
    })
    public ResponseEntity<FeedbackResponse> createFeedback(
            @Valid @RequestBody FeedbackRequest request
    ) {
        FeedbackResponse response = feedbackMapper.toResponse(
                feedbackService.createFeedback(request)
        );

        return ResponseEntity
                .created(URI.create("/feedback/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(
            summary = "List feedback",
            description = "Returns feedback entries using pagination and sorting"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Feedback returned successfully"
    )
    public ResponseEntity<PageResponse<FeedbackResponse>> getAllFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        Sort.Direction sortDirection =
                "asc".equalsIgnoreCase(direction)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        String safeSortField = validateSortField(sortBy);

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(sortDirection, safeSortField)
        );

        Page<FeedbackResponse> result = feedbackService
                .getAllFeedback(pageable)
                .map(feedbackMapper::toResponse);

        return ResponseEntity.ok(
                PageResponse.from(result)
        );
    }

    private String validateSortField(String sortBy) {
        return switch (sortBy) {
            case "id",
                 "name",
                 "email",
                 "createdAt" -> sortBy;

            default -> "createdAt";
        };
    }
}