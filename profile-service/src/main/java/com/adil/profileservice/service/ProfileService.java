package com.adil.profileservice.service;

import com.adil.profileservice.dto.ProfileRequest;
import com.adil.profileservice.exception.DuplicateProfileEmailException;
import com.adil.profileservice.exception.ProfileNotFoundException;
import com.adil.profileservice.model.Profile;
import com.adil.profileservice.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public Profile createProfile(ProfileRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());

        if (profileRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new DuplicateProfileEmailException(normalizedEmail);

        }

        Profile profile = new Profile(
                request.getName().trim(),
                normalizedEmail,
                normalizeBio(request.getBio())
        );

        return profileRepository.save(profile);
    }

    public Page<Profile> getAllProfiles(Pageable pageable) {
        return profileRepository.findAll(pageable);
    }

    public Profile getProfileById(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new ProfileNotFoundException(id));
    }

    @Transactional
    public Profile updateProfile(
            Long id,
            ProfileRequest request
    ) {
        Profile profile = getProfileById(id);
        String normalizedEmail = normalizeEmail(request.getEmail());

        boolean emailAlreadyUsed =
                profileRepository.existsByEmailIgnoreCaseAndIdNot(
                        normalizedEmail,
                        id
                );

        if (emailAlreadyUsed) {
            throw new DuplicateProfileEmailException(normalizedEmail);
        }

        profile.changeName(request.getName().trim());
        profile.changeEmail(normalizedEmail);
        profile.changeBio(normalizeBio(request.getBio()));

        return profile;
    }

    @Transactional
    public void deleteProfile(Long id) {
        Profile profile = getProfileById(id);
        profileRepository.delete(profile);
    }

    private String normalizeEmail(String email) {
        return email
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeBio(String bio) {
        if (bio == null) {
            return null;
        }

        String normalizedBio = bio.trim();

        return normalizedBio.isEmpty()
                ? null
                : normalizedBio;
    }
}