package com.adil.profileservice.controller;

import com.adil.profileservice.dto.PageResponse;
import com.adil.profileservice.dto.ProfileRequest;
import com.adil.profileservice.dto.ProfileResponse;
import com.adil.profileservice.mapper.ProfileMapper;
import com.adil.profileservice.service.ProfileService;
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
@RequestMapping("/profiles")
@Tag(
        name = "Profiles",
        description = "Operations for creating, reading, updating and deleting profiles"
)
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileMapper profileMapper;

    public ProfileController(
            ProfileService profileService,
            ProfileMapper profileMapper
    ) {
        this.profileService = profileService;
        this.profileMapper = profileMapper;
    }

    @PostMapping
    @Operation(
            summary = "Create profile",
            description = "Creates a new user profile"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Profile created successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already exists"
            )
    })
    public ResponseEntity<ProfileResponse> createProfile(
            @Valid @RequestBody ProfileRequest request
    ) {
        ProfileResponse response = profileMapper.toResponse(
                profileService.createProfile(request)
        );

        return ResponseEntity
                .created(URI.create("/profiles/" + response.id()))
                .body(response);
    }

    @GetMapping
    @Operation(
            summary = "List profiles",
            description = "Returns profiles using pagination and sorting"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Profiles returned successfully"
    )
    public ResponseEntity<PageResponse<ProfileResponse>> getAllProfiles(
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

        Sort sort = Sort.by(
                sortDirection,
                validateSortField(sortBy)
        );

        PageRequest pageable = PageRequest.of(
                safePage,
                safeSize,
                sort
        );

        Page<ProfileResponse> result = profileService
                .getAllProfiles(pageable)
                .map(profileMapper::toResponse);

        return ResponseEntity.ok(
                PageResponse.from(result)
        );
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get profile by ID",
            description = "Returns a profile by its unique identifier"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile found"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profile not found"
            )
    })
    public ResponseEntity<ProfileResponse> getProfileById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                profileMapper.toResponse(
                        profileService.getProfileById(id)
                )
        );
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update profile",
            description = "Updates an existing profile"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Profile updated successfully"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profile not found"
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email already exists"
            )
    })
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.ok(
                profileMapper.toResponse(
                        profileService.updateProfile(id, request)
                )
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete profile",
            description = "Deletes an existing profile"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Profile deleted successfully"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Profile not found"
            )
    })
    public ResponseEntity<Void> deleteProfile(
            @PathVariable Long id
    ) {
        profileService.deleteProfile(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    private String validateSortField(String sortBy) {
        return switch (sortBy) {
            case "id",
                 "name",
                 "email",
                 "createdAt",
                 "updatedAt" -> sortBy;

            default -> "createdAt";
        };
    }
}