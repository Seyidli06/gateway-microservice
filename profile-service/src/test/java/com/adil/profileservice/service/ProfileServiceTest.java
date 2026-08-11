package com.adil.profileservice.service;

import com.adil.profileservice.dto.ProfileRequest;
import com.adil.profileservice.exception.DuplicateProfileEmailException;
import com.adil.profileservice.exception.ProfileNotFoundException;
import com.adil.profileservice.model.Profile;
import com.adil.profileservice.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileService(profileRepository);
    }

    @Test
    void createProfile_shouldSaveProfileWithNormalizedValues() {
        ProfileRequest request = new ProfileRequest(
                "  Adil Mammadov  ",
                "  ADIL@EXAMPLE.COM  ",
                "  Java Backend Developer  "
        );

        when(profileRepository.existsByEmailIgnoreCase("adil@example.com"))
                .thenReturn(false);

        when(profileRepository.save(any(Profile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Profile result = profileService.createProfile(request);

        assertEquals("Adil Mammadov", result.getName());
        assertEquals("adil@example.com", result.getEmail());
        assertEquals("Java Backend Developer", result.getBio());

        verify(profileRepository)
                .existsByEmailIgnoreCase("adil@example.com");

        verify(profileRepository)
                .save(any(Profile.class));
    }

    @Test
    void createProfile_shouldThrowWhenEmailAlreadyExists() {
        ProfileRequest request = new ProfileRequest(
                "Adil",
                "ADIL@EXAMPLE.COM",
                "Bio"
        );

        when(profileRepository.existsByEmailIgnoreCase("adil@example.com"))
                .thenReturn(true);

        assertThrows(
                DuplicateProfileEmailException.class,
                () -> profileService.createProfile(request)
        );

        verify(profileRepository, never())
                .save(any(Profile.class));
    }

    @Test
    void getProfileById_shouldReturnProfileWhenFound() {
        Profile profile = new Profile(
                "Adil",
                "adil@example.com",
                "Backend Developer"
        );

        when(profileRepository.findById(1L))
                .thenReturn(Optional.of(profile));

        Profile result = profileService.getProfileById(1L);

        assertSame(profile, result);

        verify(profileRepository).findById(1L);
    }

    @Test
    void getProfileById_shouldThrowWhenProfileDoesNotExist() {
        when(profileRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProfileNotFoundException.class,
                () -> profileService.getProfileById(99L)
        );
    }

    @Test
    void getAllProfiles_shouldReturnPage() {
        PageRequest pageable = PageRequest.of(0, 20);

        Profile profile1 = new Profile(
                "Adil",
                "adil@example.com",
                "Developer"
        );

        Profile profile2 = new Profile(
                "Nigar",
                "nigar@example.com",
                "Designer"
        );

        Page<Profile> expectedPage = new PageImpl<>(
                List.of(profile1, profile2),
                pageable,
                2
        );

        when(profileRepository.findAll(pageable))
                .thenReturn(expectedPage);

        Page<Profile> result =
                profileService.getAllProfiles(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(profileRepository).findAll(pageable);
    }

    @Test
    void updateProfile_shouldUpdateExistingProfile() {
        Profile existingProfile = new Profile(
                "Old Name",
                "old@example.com",
                "Old Bio"
        );

        ProfileRequest request = new ProfileRequest(
                "  New Name  ",
                "  NEW@EXAMPLE.COM ",
                "  New Bio  "
        );

        when(profileRepository.findById(1L))
                .thenReturn(Optional.of(existingProfile));

        when(profileRepository.existsByEmailIgnoreCaseAndIdNot(
                "new@example.com",
                1L
        )).thenReturn(false);

        Profile result =
                profileService.updateProfile(1L, request);

        assertEquals("New Name", result.getName());
        assertEquals("new@example.com", result.getEmail());
        assertEquals("New Bio", result.getBio());

        verify(profileRepository)
                .existsByEmailIgnoreCaseAndIdNot(
                        "new@example.com",
                        1L
                );
    }

    @Test
    void updateProfile_shouldThrowWhenEmailBelongsToAnotherProfile() {
        Profile existingProfile = new Profile(
                "Adil",
                "adil@example.com",
                "Bio"
        );

        ProfileRequest request = new ProfileRequest(
                "Adil",
                "other@example.com",
                "Bio"
        );

        when(profileRepository.findById(1L))
                .thenReturn(Optional.of(existingProfile));

        when(profileRepository.existsByEmailIgnoreCaseAndIdNot(
                "other@example.com",
                1L
        )).thenReturn(true);

        assertThrows(
                DuplicateProfileEmailException.class,
                () -> profileService.updateProfile(1L, request)
        );
    }

    @Test
    void deleteProfile_shouldDeleteExistingProfile() {
        Profile profile = new Profile(
                "Adil",
                "adil@example.com",
                "Bio"
        );

        when(profileRepository.findById(1L))
                .thenReturn(Optional.of(profile));

        profileService.deleteProfile(1L);

        verify(profileRepository).delete(profile);
    }

    @Test
    void deleteProfile_shouldThrowWhenProfileDoesNotExist() {
        when(profileRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ProfileNotFoundException.class,
                () -> profileService.deleteProfile(99L)
        );

        verify(profileRepository, never())
                .delete(any(Profile.class));
    }
}