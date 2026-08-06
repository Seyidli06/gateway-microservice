package com.adil.profileservice.service;

import com.adil.profileservice.dto.ProfileRequest;
import com.adil.profileservice.model.Profile;
import com.adil.profileservice.repository.ProfileRepository;
import com.adil.profileservice.exception.ProfileNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Profile createProfile(ProfileRequest request) {
        Profile profile = new Profile(
                null,
                request.getName(),
                request.getEmail(),
                request.getBio()
        );

        return profileRepository.save(profile);
    }

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Profile getProfileById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
    }

    public Profile updateProfile(Long id, ProfileRequest request) {
        Profile profile = getProfileById(id);

        profile.setName(request.getName());
        profile.setEmail(request.getEmail());
        profile.setBio(request.getBio());

        return profileRepository.save(profile);
    }

    public void deleteProfile(Long id) {
        if (!profileRepository.existsById(id)) {
            throw new ProfileNotFoundException(id);
        }

        profileRepository.deleteById(id);
    }
}