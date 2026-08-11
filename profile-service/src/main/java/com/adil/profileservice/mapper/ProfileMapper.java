package com.adil.profileservice.mapper;

import com.adil.profileservice.dto.ProfileResponse;
import com.adil.profileservice.model.Profile;
import org.springframework.stereotype.Component;

@Component
public class ProfileMapper {

    public ProfileResponse toResponse(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getName(),
                profile.getEmail(),
                profile.getBio(),
                profile.getVersion(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}