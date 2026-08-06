package com.adil.profileservice.controller;

import com.adil.profileservice.dto.ProfileRequest;
import com.adil.profileservice.model.Profile;
import com.adil.profileservice.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/profiles")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @PostMapping
    public ResponseEntity<Profile> createProfile(
            @Valid @RequestBody ProfileRequest request
    ) {
        Profile createdProfile = profileService.createProfile(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(createdProfile);
    }

    @GetMapping
    public ResponseEntity<List<Profile>> getAllProfiles() {
        return ResponseEntity.ok(profileService.getAllProfiles());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Profile> getProfileById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(profileService.getProfileById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Profile> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody ProfileRequest request
    ) {
        return ResponseEntity.ok(
                profileService.updateProfile(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProfile(
            @PathVariable Long id
    ) {
        profileService.deleteProfile(id);

        return ResponseEntity.noContent().build();
    }
}