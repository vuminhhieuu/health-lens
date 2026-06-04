package com.healthlens.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthlens.api.audit.AuditActions;
import com.healthlens.api.audit.AuditResourceTypes;
import com.healthlens.api.dto.MetricClassificationDto;
import com.healthlens.api.dto.ReferenceRangeDto;
import com.healthlens.api.dto.request.CreateProfileRequest;
import com.healthlens.api.dto.request.UpdateProfileRequest;
import com.healthlens.api.dto.response.ProfileResponse;
import com.healthlens.api.dto.response.SharedProfileResponse;
import com.healthlens.api.entity.HealthRecord;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.ProfileShare;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ProfileLimitExceededException;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.HealthRecordRepository;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.ProfileShareRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

        @Mock
        private ProfileRepository profileRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private ProfileShareRepository profileShareRepository;

        @Mock
        private HealthRecordRepository healthRecordRepository;

        @Mock
        private com.healthlens.api.audit.AuditEventRecorder auditEventRecorder;

        @Mock
        private ReferenceDataService referenceDataService;

        @Mock
        private StorageService storageService;

        private ProfileService profileService;

        private User testUser;
        private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        profileService = new ProfileService(
                profileRepository,
                profileShareRepository,
                healthRecordRepository,
                userRepository,
                referenceDataService,
                new ObjectMapper(),
                auditEventRecorder,
                storageService
        );
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

                HealthRecord latest = new HealthRecord();
                latest.setId(UUID.randomUUID());
                latest.setProfileId(profile.getId());
                latest.setStatus("done");
                latest.setMetrics("[{\"name\":\"glucose\",\"status\":\"abnormal\"}]");

                when(profileRepository.findAllByUserId(userId)).thenReturn(List.of(profile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profile.getId())))
                                .thenReturn(List.of(latest));

                List<ProfileResponse> responses = profileService.getProfiles(userId);

                assertThat(responses).hasSize(1);
                ProfileResponse response = responses.get(0);
                assertThat(response.id()).isEqualTo(profile.getId());
                assertThat(response.displayName()).isEqualTo("Family Member");
                assertThat(response.gender()).isEqualTo("female");
                assertThat(response.latestStatus()).isEqualTo("abnormal");
        }

        @Test
        void getProfiles_shouldReclassifyNormalRawMetricAgainstReferenceData() {
                Profile profile = new Profile();
                profile.setId(UUID.randomUUID());
                profile.setDisplayName("Family Member");
                profile.setCreatedAt(Instant.now());
                profile.setUpdatedAt(Instant.now());

                HealthRecord latest = new HealthRecord();
                latest.setId(UUID.randomUUID());
                latest.setProfileId(profile.getId());
                latest.setStatus("done");
                latest.setMetrics(
                                "[{\"name\":\"glucose\",\"value\":\"8.1\",\"normalizedValue\":\"8.1\",\"status\":\"normal\"}]");

                when(profileRepository.findAllByUserId(userId)).thenReturn(List.of(profile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profile.getId())))
                                .thenReturn(List.of(latest));
                when(referenceDataService.classifyMetricWithoutAudit(
                                any(),
                                any(),
                                any(),
                                any())).thenReturn(new MetricClassificationDto("abnormal", null, null, null));

                List<ProfileResponse> responses = profileService.getProfiles(userId);

                assertThat(responses).hasSize(1);
                assertThat(responses.getFirst().latestStatus()).isEqualTo("abnormal");
        }

        @Test
        void getProfiles_shouldReclassifyNoDataRawMetricWhenValueIsPresent() {
                Profile profile = new Profile();
                profile.setId(UUID.randomUUID());
                profile.setDisplayName("Family Member");
                profile.setCreatedAt(Instant.now());
                profile.setUpdatedAt(Instant.now());

                HealthRecord latest = new HealthRecord();
                latest.setId(UUID.randomUUID());
                latest.setProfileId(profile.getId());
                latest.setStatus("done");
                latest.setMetrics("""
                                [
                                  {"name":"glucose","value":"8.1","normalizedValue":"8.1","status":"no_data"},
                                  {"name":"hemoglobin","value":"14","normalizedValue":"14","status":"normal"}
                                ]
                                """);

                when(profileRepository.findAllByUserId(userId)).thenReturn(List.of(profile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profile.getId())))
                                .thenReturn(List.of(latest));
                when(referenceDataService.classifyMetricWithoutAudit(
                                any(),
                                any(),
                                any(),
                                any())).thenAnswer(invocation -> {
                                        String metricName = invocation.getArgument(0);
                                        if ("glucose".equals(metricName)) {
                                                return new MetricClassificationDto("abnormal", null, null, null);
                                        }
                                        return new MetricClassificationDto("normal", null, null, null);
                                });

                List<ProfileResponse> responses = profileService.getProfiles(userId);

                assertThat(responses).hasSize(1);
                assertThat(responses.getFirst().latestStatus()).isEqualTo("abnormal");
        }

        @Test
        void getProfiles_shouldUseDocumentReferenceRangeBeforeRawNormalStatus() throws Exception {
                Profile profile = new Profile();
                profile.setId(UUID.randomUUID());
                profile.setDisplayName("Family Member");
                profile.setCreatedAt(Instant.now());
                profile.setUpdatedAt(Instant.now());

                com.healthlens.api.dto.MetricDto metric = com.healthlens.api.dto.MetricDto.builder()
                                .name("glucose")
                                .value("8.1")
                                .normalizedValue("8.1")
                                .status("normal")
                                .referenceRange(new ReferenceRangeDto(
                                                BigDecimal.valueOf(3.9),
                                                BigDecimal.valueOf(6.4),
                                                BigDecimal.valueOf(3.2),
                                                BigDecimal.valueOf(7.1),
                                                "mmol/L"))
                                .build();

                HealthRecord latest = new HealthRecord();
                latest.setId(UUID.randomUUID());
                latest.setProfileId(profile.getId());
                latest.setStatus("done");
                latest.setMetrics(new ObjectMapper().writeValueAsString(List.of(metric)));

                when(profileRepository.findAllByUserId(userId)).thenReturn(List.of(profile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profile.getId())))
                                .thenReturn(List.of(latest));

                List<ProfileResponse> responses = profileService.getProfiles(userId);

                assertThat(responses).hasSize(1);
                assertThat(responses.getFirst().latestStatus()).isEqualTo("abnormal");
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
                                "  Some notes  ",
                                "  Tiểu đường  ",
                                "  Aspirin  ",
                                "  Hải sản  ");

                ProfileResponse response = profileService.createProfile(userId, request);

                assertThat(response.displayName()).isEqualTo("Member Name");
                assertThat(response.notes()).isEqualTo("Some notes");
                assertThat(response.chronicConditions()).isEqualTo("Tiểu đường");
                assertThat(response.currentMedications()).isEqualTo("Aspirin");
                assertThat(response.allergies()).isEqualTo("Hải sản");
                verify(profileRepository).save(any(Profile.class));
        }

        @Test
        void updateProfile_PersistsClinicalFields() {
                UUID profileId = UUID.randomUUID();
                Profile profile = new Profile();
                profile.setId(profileId);
                profile.setUser(testUser);
                profile.setDisplayName("Mẹ");
                profile.setDefault(false);

                when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
                when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

                UpdateProfileRequest request = new UpdateProfileRequest(
                                "Mẹ",
                                LocalDate.of(1960, 3, 3),
                                "female",
                                "Ghi chú chung",
                                "Cao huyết áp",
                                "Amlodipine",
                                "Penicillin");

                ProfileResponse response = profileService.updateProfile(userId, profileId, request);

                assertThat(profile.getChronicConditions()).isEqualTo("Cao huyết áp");
                assertThat(profile.getCurrentMedications()).isEqualTo("Amlodipine");
                assertThat(profile.getAllergies()).isEqualTo("Penicillin");
                assertThat(response.chronicConditions()).isEqualTo("Cao huyết áp");
        }

        @Test
        void updateProfile_PreservesClinicalFieldsOnPartialUpdate() {
                UUID profileId = UUID.randomUUID();
                Profile profile = new Profile();
                profile.setId(profileId);
                profile.setUser(testUser);
                profile.setDisplayName("Mẹ");
                profile.setChronicConditions("Cao huyết áp");
                profile.setCurrentMedications("Amlodipine");
                profile.setAllergies("Penicillin");
                profile.setDefault(false);

                when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
                when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

                UpdateProfileRequest request = new UpdateProfileRequest(
                                "Mẹ đổi tên",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);

                profileService.updateProfile(userId, profileId, request);

                assertThat(profile.getChronicConditions()).isEqualTo("Cao huyết áp");
                assertThat(profile.getCurrentMedications()).isEqualTo("Amlodipine");
                assertThat(profile.getAllergies()).isEqualTo("Penicillin");
        }

        @Test
        void updateProfile_ClearsClinicalFieldsWhenEmpty() {
                UUID profileId = UUID.randomUUID();
                Profile profile = new Profile();
                profile.setId(profileId);
                profile.setUser(testUser);
                profile.setDisplayName("Mẹ");
                profile.setChronicConditions("Cao huyết áp");
                profile.setCurrentMedications("Amlodipine");
                profile.setAllergies("Penicillin");
                profile.setDefault(false);

                when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
                when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

                UpdateProfileRequest request = new UpdateProfileRequest(
                                "Mẹ",
                                null,
                                null,
                                null,
                                "",
                                "  ",
                                "");

                ProfileResponse response = profileService.updateProfile(userId, profileId, request);

                assertThat(profile.getChronicConditions()).isNull();
                assertThat(profile.getCurrentMedications()).isNull();
                assertThat(profile.getAllergies()).isNull();
                assertThat(response.chronicConditions()).isNull();
        }

        @Test
        void createProfile_LimitExceeded_ShouldThrowException() {
                when(profileRepository.countByUserId(userId)).thenReturn(10L);

                CreateProfileRequest request = new CreateProfileRequest(
                                "Extra Member",
                                LocalDate.of(1990, 1, 1),
                                "other",
                                null,
                                null,
                                null,
                                null);

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
                                null,
                                null,
                                null,
                                null);

                assertThatThrownBy(() -> profileService.createProfile(userId, request))
                                .isInstanceOf(ResourceNotFoundException.class)
                                .hasMessageContaining("Người dùng không tồn tại");
        }

        @Test
        void deleteProfile_OwnedFamilyProfile_ShouldDeleteAndAudit() {
                UUID profileId = UUID.randomUUID();
                Profile profile = new Profile();
                profile.setId(profileId);
                profile.setUser(testUser);
                profile.setDisplayName("Mẹ");
                profile.setDefault(false);

                when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
                when(healthRecordRepository.findFileKeysByProfileIdAndUserId(profileId, userId))
                                .thenReturn(List.of(
                                                "health-records/" + userId + "/" + profileId + "/record-a/original.pdf",
                                                "health-records/" + userId + "/" + profileId + "/record-b/original.jpg"));
                when(storageService.deleteObjects(any())).thenReturn(2);
                when(storageService.deleteObjectsByPrefix("health-records/" + userId + "/" + profileId + "/"))
                                .thenReturn(0);

                profileService.deleteProfile(userId, profileId);

                verify(storageService).deleteObjects(List.of(
                                "health-records/" + userId + "/" + profileId + "/record-a/original.pdf",
                                "health-records/" + userId + "/" + profileId + "/record-b/original.jpg"));
                verify(storageService).deleteObjectsByPrefix("health-records/" + userId + "/" + profileId + "/");
                verify(profileRepository).delete(profile);
                verify(auditEventRecorder).recordEvent(
                                userId,
                                AuditActions.DELETE_PROFILE,
                                AuditResourceTypes.PROFILE,
                                profileId,
                                Map.of(
                                                "displayName", "Mẹ",
                                                "healthRecordFiles", 2,
                                                "exactFilesDeleted", 2,
                                                "prefixFilesDeleted", 0));
        }

        @Test
        void deleteProfile_StorageDeleteFails_ShouldNotDeleteDatabaseRow() {
                UUID profileId = UUID.randomUUID();
                Profile profile = new Profile();
                profile.setId(profileId);
                profile.setUser(testUser);
                profile.setDisplayName("Mẹ");
                profile.setDefault(false);
                List<String> fileKeys = List.of(
                                "health-records/" + userId + "/" + profileId + "/record-a/original.pdf");

                when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));
                when(healthRecordRepository.findFileKeysByProfileIdAndUserId(profileId, userId))
                                .thenReturn(fileKeys);
                when(storageService.deleteObjects(fileKeys))
                                .thenThrow(new IllegalStateException("Failed to delete explicit storage objects"));

                assertThatThrownBy(() -> profileService.deleteProfile(userId, profileId))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessageContaining("Failed to delete explicit storage objects");
                verify(profileRepository, never()).delete(any());
                verify(auditEventRecorder, never()).recordEvent(any(), any(), any(), any(), any());
        }

        @Test
        void deleteProfile_DefaultProfile_ShouldThrow() {
                UUID profileId = UUID.randomUUID();
                Profile profile = new Profile();
                profile.setId(profileId);
                profile.setUser(testUser);
                profile.setDisplayName("Tôi");
                profile.setDefault(true);

                when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

                assertThatThrownBy(() -> profileService.deleteProfile(userId, profileId))
                                .isInstanceOf(IllegalArgumentException.class)
                                .hasMessageContaining("Không thể xóa hồ sơ mặc định");
                verify(storageService, never()).deleteObjects(any());
                verify(profileRepository, never()).delete(any());
                verify(auditEventRecorder, never()).recordEvent(any(), any(), any(), any(), any());
        }

        @Test
        void deleteProfile_NonOwner_ShouldThrowAccessDenied() {
                UUID profileId = UUID.randomUUID();
                User owner = new User();
                owner.setId(UUID.randomUUID());
                Profile profile = new Profile();
                profile.setId(profileId);
                profile.setUser(owner);
                profile.setDisplayName("Bố");
                profile.setDefault(false);

                when(profileRepository.findById(profileId)).thenReturn(Optional.of(profile));

                assertThatThrownBy(() -> profileService.deleteProfile(userId, profileId))
                                .isInstanceOf(AccessDeniedException.class)
                                .hasMessageContaining("Chỉ chủ tài khoản mới có thể xóa hồ sơ này");
                verify(storageService, never()).deleteObjects(any());
                verify(profileRepository, never()).delete(any());
                verify(auditEventRecorder, never()).recordEvent(any(), any(), any(), any(), any());
        }

        @Test
        void getSharedProfiles_shouldUseLatestRecordHealthStatusAndUpdatedAt() {
                UUID viewerId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                UUID profileId = UUID.randomUUID();

                ProfileShare share = new ProfileShare();
                share.setId(UUID.randomUUID());
                share.setProfileId(profileId);
                share.setViewerId(viewerId);
                share.setOwnerId(ownerId);
                share.setAccessLevel("edit");
                share.setGrantedAt(Instant.parse("2026-05-01T00:00:00Z"));

                User owner = new User();
                owner.setId(ownerId);
                Profile sharedProfile = new Profile();
                sharedProfile.setId(profileId);
                sharedProfile.setDisplayName("Mẹ");
                sharedProfile.setUser(owner);
                sharedProfile.setUpdatedAt(Instant.parse("2026-05-05T08:00:00Z"));

                HealthRecord latest = new HealthRecord();
                latest.setId(UUID.randomUUID());
                latest.setProfileId(profileId);
                latest.setStatus("done");
                latest.setMetrics("[{\"name\":\"glucose\",\"status\":\"attention\"}]");
                latest.setUpdatedAt(Instant.parse("2026-05-06T10:30:00Z"));

                when(profileShareRepository.findAllByViewerIdAndRevokedAtIsNull(viewerId)).thenReturn(List.of(share));
                when(profileRepository.findAllById(List.of(profileId))).thenReturn(List.of(sharedProfile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profileId)))
                                .thenReturn(List.of(latest));

                List<SharedProfileResponse> responses = profileService.getSharedProfiles(viewerId);

                assertThat(responses).hasSize(1);
                SharedProfileResponse response = responses.getFirst();
                assertThat(response.profileId()).isEqualTo(profileId);
                assertThat(response.displayName()).isEqualTo("Mẹ");
                assertThat(response.accessLevel()).isEqualTo("edit");
                assertThat(response.latestStatus()).isEqualTo("attention");
                assertThat(response.lastUpdated()).isEqualTo(Instant.parse("2026-05-06T10:30:00Z"));
        }

        @Test
        void getSharedProfiles_shouldReturnUnverifiedAndProfileUpdatedAtWhenNoRecord() {
                UUID viewerId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                UUID profileId = UUID.randomUUID();

                ProfileShare share = new ProfileShare();
                share.setId(UUID.randomUUID());
                share.setProfileId(profileId);
                share.setViewerId(viewerId);
                share.setOwnerId(ownerId);
                share.setAccessLevel("view");

                User owner = new User();
                owner.setId(ownerId);
                Profile sharedProfile = new Profile();
                sharedProfile.setId(profileId);
                sharedProfile.setDisplayName("Bố");
                sharedProfile.setUser(owner);
                sharedProfile.setUpdatedAt(Instant.parse("2026-05-04T12:00:00Z"));

                when(profileShareRepository.findAllByViewerIdAndRevokedAtIsNull(viewerId)).thenReturn(List.of(share));
                when(profileRepository.findAllById(List.of(profileId))).thenReturn(List.of(sharedProfile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profileId)))
                                .thenReturn(List.of());

                List<SharedProfileResponse> responses = profileService.getSharedProfiles(viewerId);

                assertThat(responses).hasSize(1);
                assertThat(responses.getFirst().latestStatus()).isEqualTo("unverified");
                assertThat(responses.getFirst().lastUpdated()).isEqualTo(Instant.parse("2026-05-04T12:00:00Z"));
        }

        @Test
        void getSharedProfiles_shouldKeepFirstSharePerProfileAndIgnoreDuplicateShares() {
                UUID viewerId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                UUID profileId = UUID.randomUUID();

                ProfileShare firstShare = new ProfileShare();
                firstShare.setId(UUID.randomUUID());
                firstShare.setProfileId(profileId);
                firstShare.setViewerId(viewerId);
                firstShare.setOwnerId(ownerId);
                firstShare.setAccessLevel("view");

                ProfileShare duplicateShare = new ProfileShare();
                duplicateShare.setId(UUID.randomUUID());
                duplicateShare.setProfileId(profileId);
                duplicateShare.setViewerId(viewerId);
                duplicateShare.setOwnerId(ownerId);
                duplicateShare.setAccessLevel("edit");

                User owner = new User();
                owner.setId(ownerId);
                Profile sharedProfile = new Profile();
                sharedProfile.setId(profileId);
                sharedProfile.setDisplayName("Ông");
                sharedProfile.setUser(owner);
                sharedProfile.setUpdatedAt(Instant.parse("2026-05-02T10:00:00Z"));

                when(profileShareRepository.findAllByViewerIdAndRevokedAtIsNull(viewerId))
                                .thenReturn(List.of(firstShare, duplicateShare));
                when(profileRepository.findAllById(List.of(profileId))).thenReturn(List.of(sharedProfile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profileId)))
                                .thenReturn(List.of());

                List<SharedProfileResponse> responses = profileService.getSharedProfiles(viewerId);

                assertThat(responses).hasSize(1);
                assertThat(responses.getFirst().profileId()).isEqualTo(profileId);
                assertThat(responses.getFirst().accessLevel()).isEqualTo("view");
        }

        @Test
        void getSharedProfiles_shouldUseFirstRecordPerProfileFromSortedBatchResult() {
                UUID viewerId = UUID.randomUUID();
                UUID ownerId = UUID.randomUUID();
                UUID profileId = UUID.randomUUID();

                ProfileShare share = new ProfileShare();
                share.setId(UUID.randomUUID());
                share.setProfileId(profileId);
                share.setViewerId(viewerId);
                share.setOwnerId(ownerId);
                share.setAccessLevel("edit");

                User owner = new User();
                owner.setId(ownerId);
                Profile sharedProfile = new Profile();
                sharedProfile.setId(profileId);
                sharedProfile.setDisplayName("Bà");
                sharedProfile.setUser(owner);
                sharedProfile.setUpdatedAt(Instant.parse("2026-05-03T08:00:00Z"));

                HealthRecord latest = new HealthRecord();
                latest.setId(UUID.randomUUID());
                latest.setProfileId(profileId);
                latest.setStatus("done");
                latest.setMetrics("[{\"name\":\"cholesterol\",\"status\":\"abnormal\"}]");
                latest.setUpdatedAt(Instant.parse("2026-05-07T11:00:00Z"));

                when(profileShareRepository.findAllByViewerIdAndRevokedAtIsNull(viewerId)).thenReturn(List.of(share));
                when(profileRepository.findAllById(List.of(profileId))).thenReturn(List.of(sharedProfile));
                when(healthRecordRepository
                                .findLatestByProfileIdsAndDeletedAtIsNullOrderByProfileIdAscExamDateDescCreatedAtDesc(
                                                List.of(profileId)))
                                .thenReturn(List.of(latest));

                List<SharedProfileResponse> responses = profileService.getSharedProfiles(viewerId);

                assertThat(responses).hasSize(1);
                assertThat(responses.getFirst().latestStatus()).isEqualTo("abnormal");
                assertThat(responses.getFirst().lastUpdated()).isEqualTo(Instant.parse("2026-05-07T11:00:00Z"));
        }

        @Test
        void getSharedProfiles_shouldSkipStaleShareWhenProfileIsMissing() {
                UUID viewerId = UUID.randomUUID();
                UUID missingProfileId = UUID.randomUUID();

                ProfileShare staleShare = new ProfileShare();
                staleShare.setId(UUID.randomUUID());
                staleShare.setProfileId(missingProfileId);
                staleShare.setViewerId(viewerId);
                staleShare.setOwnerId(UUID.randomUUID());
                staleShare.setAccessLevel("view");

                when(profileShareRepository.findAllByViewerIdAndRevokedAtIsNull(viewerId)).thenReturn(List.of(staleShare));
                when(profileRepository.findAllById(List.of(missingProfileId))).thenReturn(List.of());

                List<SharedProfileResponse> responses = profileService.getSharedProfiles(viewerId);

                assertThat(responses).isEmpty();
        }
}
