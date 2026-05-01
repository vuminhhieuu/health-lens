package com.healthlens.api.service;

import com.healthlens.api.dto.request.CreateProfileRequest;
import com.healthlens.api.dto.request.UpdateProfileRequest;
import com.healthlens.api.dto.response.ProfileResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ProfileLimitExceededException;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    private User testUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
    }

    @Test
    void getProfiles_ShouldReturnMappedResponses() {
        Profile profile = new Profile();
        profile.setId(UUID.randomUUID());
        profile.setDisplayName("Family Member");
        profile.setBirthDate(LocalDate.of(1995, 5, 5));
        profile.setGender("female");
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());

        when(profileRepository.findAllByUserId(userId)).thenReturn(List.of(profile));

        List<ProfileResponse> responses = profileService.getProfiles(userId);

        assertThat(responses).hasSize(1);
        ProfileResponse response = responses.get(0);
        assertThat(response.id()).isEqualTo(profile.getId());
        assertThat(response.displayName()).isEqualTo("Family Member");
        assertThat(response.gender()).isEqualTo("female");
    }

    @Test
    void createProfile_Success_ShouldTrimFields() {
        when(profileRepository.countByUserId(userId)).thenReturn(0L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> {
            Profile p = i.getArgument(0);
            p.setId(UUID.randomUUID());
            p.setCreatedAt(Instant.now());
            p.setUpdatedAt(Instant.now());
            return p;
        });

        CreateProfileRequest request = new CreateProfileRequest(
                "  Member Name  ",
                LocalDate.of(1990, 1, 1),
                "male",
                "  Some notes  "
        );

        ProfileResponse response = profileService.createProfile(userId, request);

        assertThat(response.displayName()).isEqualTo("Member Name");
        assertThat(response.notes()).isEqualTo("Some notes");
        verify(profileRepository).save(any(Profile.class));
    }

    @Test
    void createProfile_LimitExceeded_ShouldThrowException() {
        when(profileRepository.countByUserId(userId)).thenReturn(10L);

        CreateProfileRequest request = new CreateProfileRequest(
                "Extra Member",
                LocalDate.of(1990, 1, 1),
                "other",
                null
        );

        assertThatThrownBy(() -> profileService.createProfile(userId, request))
                .isInstanceOf(ProfileLimitExceededException.class)
                .hasMessageContaining("Đã đạt giới hạn 10 hồ sơ");
    }

    @Test
    void createProfile_UserNotFound_ShouldThrowException() {
        when(profileRepository.countByUserId(userId)).thenReturn(0L);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        CreateProfileRequest request = new CreateProfileRequest(
                "Member Name",
                LocalDate.of(1990, 1, 1),
                "male",
                null
        );

        assertThatThrownBy(() -> profileService.createProfile(userId, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User khong ton tai");
    }
}
