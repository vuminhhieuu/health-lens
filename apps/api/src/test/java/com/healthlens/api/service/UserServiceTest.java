package com.healthlens.api.service;

import com.healthlens.api.audit.AuditEventRecorder;
import com.healthlens.api.dto.request.UpdateHealthContextRequest;
import com.healthlens.api.dto.request.UpdateUserRequest;
import com.healthlens.api.dto.response.UserResponse;
import com.healthlens.api.entity.Profile;
import com.healthlens.api.entity.User;
import com.healthlens.api.exception.ResourceNotFoundException;
import com.healthlens.api.repository.ProfileRepository;
import com.healthlens.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private AuditEventRecorder auditEventRecorder;

    @Mock
    private StorageService storageService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("test@example.com");
        testUser.setFullName("Old Name");
        testUser.setBirthDate(LocalDate.of(1990, 1, 1));
        testUser.setGender("male");
        testUser.setEmailVerified(true);
    }

    @Test
    void getCurrentUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        testUser.setAvatarStorageKey("avatars/%s/avatar.png".formatted(userId));
        testUser.setAvatarContentType("image/png");
        testUser.setAvatarSizeBytes(128L);
        when(storageService.generateDownloadUrl("avatars/%s/avatar.png".formatted(userId), Duration.ofMinutes(15)))
                .thenReturn("https://storage.local/avatar.png?signature=short");

        UserResponse response = userService.getCurrentUser(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.fullName()).isEqualTo("Old Name");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(response.gender()).isEqualTo("male");
        assertThat(response.avatarUrl()).isEqualTo("https://storage.local/avatar.png?signature=short");
    }

    @Test
    void getCurrentUser_NotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Người dùng không tồn tại");
    }

    @Test
    void updateCurrentUser_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest(
                "New Name  ",
                LocalDate.of(1995, 5, 5),
                "other",
                null,
                null);

        UserResponse response = userService.updateCurrentUser(userId, request);

        assertThat(response.fullName()).isEqualTo("New Name"); // should be trimmed
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1995, 5, 5));
        assertThat(response.gender()).isEqualTo("other");

        verify(userRepository).save(testUser);
    }

    @Test
    void updateCurrentUser_PartialUpdate_Success() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());

        // Update only full name
        UpdateUserRequest request = new UpdateUserRequest("Just Name", null, null, null, null);

        UserResponse response = userService.updateCurrentUser(userId, request);

        assertThat(response.fullName()).isEqualTo("Just Name");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1990, 1, 1)); // unchanged
        assertThat(response.gender()).isEqualTo("male"); // unchanged

        verify(userRepository).save(testUser);
    }

    @Test
    void updateCurrentUser_NotFound_ThrowsException() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest("Name", LocalDate.of(1995, 5, 5), "other", null, null);

        assertThatThrownBy(() -> userService.updateCurrentUser(userId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void uploadAvatar_SuccessStoresMetadataAndReturnsSignedUrl() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] { 1, 2, 3, 4 });
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(storageService.generateDownloadUrl(any(String.class), any(Duration.class)))
                .thenReturn("https://storage.local/avatar.png?signature=short");

        UserResponse response = userService.uploadAvatar(userId, file);

        assertThat(testUser.getAvatarStorageKey()).startsWith("avatars/" + userId + "/");
        assertThat(testUser.getAvatarContentType()).isEqualTo("image/png");
        assertThat(testUser.getAvatarSizeBytes()).isEqualTo(4L);
        assertThat(testUser.getAvatarUpdatedAt()).isNotNull();
        assertThat(response.avatarUrl()).isEqualTo("https://storage.local/avatar.png?signature=short");
        verify(storageService).uploadObject(testUser.getAvatarStorageKey(), file.getBytes(), "image/png");
        verify(userRepository).save(testUser);
    }

    @Test
    void uploadAvatar_RejectsUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                new byte[] { 1 });

        assertThatThrownBy(() -> userService.uploadAvatar(userId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Định dạng ảnh đại diện không được hỗ trợ");
    }

    @Test
    void uploadAvatar_RejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[0]);

        assertThatThrownBy(() -> userService.uploadAvatar(userId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ảnh đại diện không được để trống");
    }

    @Test
    void uploadAvatar_RejectsOversizedFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[(int) UserService.MAX_AVATAR_BYTES + 1]);

        assertThatThrownBy(() -> userService.uploadAvatar(userId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ảnh đại diện tối đa");
    }

    @Test
    void uploadAvatar_RejectsUnsafeFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../avatar.png",
                "image/png",
                new byte[] { 1 });

        assertThatThrownBy(() -> userService.uploadAvatar(userId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tên tệp ảnh đại diện không an toàn");
    }

    @Test
    void uploadAvatar_StorageFailureDoesNotPersistMetadata() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] { 1, 2 });
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        org.mockito.Mockito.doThrow(new RuntimeException("storage down"))
                .when(storageService).uploadObject(any(String.class), any(byte[].class), any(String.class));

        assertThatThrownBy(() -> userService.uploadAvatar(userId, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Không thể lưu ảnh đại diện");

        assertThat(testUser.getAvatarStorageKey()).isNull();
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).save(any(User.class));
    }

    @Test
    void uploadAvatar_ReplacesPreviousAvatarWithBestEffortCleanup() {
        testUser.setAvatarStorageKey("avatars/%s/old.png".formatted(userId));
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.webp",
                "image/webp",
                new byte[] { 1, 2, 3 });
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        userService.uploadAvatar(userId, file);

        verify(storageService).deleteObject("avatars/%s/old.png".formatted(userId));
        assertThat(testUser.getAvatarStorageKey()).startsWith("avatars/" + userId + "/");
        assertThat(testUser.getAvatarContentType()).isEqualTo("image/webp");
    }

    @Test
    void updateCurrentUser_PersistsPersonalDescriptionAndNotes() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest(
                "New Name",
                null,
                null,
                "  Mô tả cá nhân  ",
                "  Ghi chú cá nhân  ");

        UserResponse response = userService.updateCurrentUser(userId, request);

        assertThat(testUser.getPersonalDescription()).isEqualTo("Mô tả cá nhân");
        assertThat(testUser.getPersonalNotes()).isEqualTo("Ghi chú cá nhân");
        assertThat(response.personalDescription()).isEqualTo("Mô tả cá nhân");
        assertThat(response.personalNotes()).isEqualTo("Ghi chú cá nhân");
    }

    @Test
    void updateCurrentUser_ClearsPersonalFieldsWhenEmpty() {
        testUser.setPersonalDescription("Old description");
        testUser.setPersonalNotes("Old notes");
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.empty());

        UpdateUserRequest request = new UpdateUserRequest(
                "New Name",
                null,
                null,
                "",
                "   ");

        UserResponse response = userService.updateCurrentUser(userId, request);

        assertThat(testUser.getPersonalDescription()).isNull();
        assertThat(testUser.getPersonalNotes()).isNull();
        assertThat(response.personalDescription()).isNull();
        assertThat(response.personalNotes()).isNull();
    }

    @Test
    void updateHealthContext_ClearsClinicalFieldsWhenEmpty() {
        Profile defaultProfile = new Profile();
        defaultProfile.setId(UUID.randomUUID());
        defaultProfile.setUser(testUser);
        defaultProfile.setDisplayName("Old Name");
        defaultProfile.setDefault(true);
        defaultProfile.setChronicConditions("Tiểu đường");
        defaultProfile.setCurrentMedications("Metformin");
        defaultProfile.setAllergies("Penicillin");

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(defaultProfile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        UpdateHealthContextRequest request = new UpdateHealthContextRequest("", "  ", null);

        UserResponse response = userService.updateHealthContext(userId, request);

        assertThat(defaultProfile.getChronicConditions()).isNull();
        assertThat(defaultProfile.getCurrentMedications()).isNull();
        assertThat(defaultProfile.getAllergies()).isNull();
        assertThat(response.chronicConditions()).isNull();
        assertThat(response.currentMedications()).isNull();
        assertThat(response.allergies()).isNull();
    }

    @Test
    void updateHealthContext_UpdatesDefaultProfileClinicalFields() {
        Profile defaultProfile = new Profile();
        defaultProfile.setId(UUID.randomUUID());
        defaultProfile.setUser(testUser);
        defaultProfile.setDisplayName("Old Name");
        defaultProfile.setDefault(true);

        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(profileRepository.findFirstByUserIdAndIsDefaultTrue(userId)).thenReturn(Optional.of(defaultProfile));
        when(profileRepository.save(any(Profile.class))).thenAnswer(i -> i.getArgument(0));

        UpdateHealthContextRequest request = new UpdateHealthContextRequest(
                "  Tiểu đường  ",
                "  Metformin  ",
                "  Penicillin  ");

        UserResponse response = userService.updateHealthContext(userId, request);

        assertThat(defaultProfile.getChronicConditions()).isEqualTo("Tiểu đường");
        assertThat(defaultProfile.getCurrentMedications()).isEqualTo("Metformin");
        assertThat(defaultProfile.getAllergies()).isEqualTo("Penicillin");
        assertThat(response.chronicConditions()).isEqualTo("Tiểu đường");
        assertThat(response.currentMedications()).isEqualTo("Metformin");
        assertThat(response.allergies()).isEqualTo("Penicillin");
    }

    @Test
    void removeAvatar_ClearsMetadataAndDeletesPreviousObject() {
        testUser.setAvatarStorageKey("avatars/%s/avatar.png".formatted(userId));
        testUser.setAvatarContentType("image/png");
        testUser.setAvatarSizeBytes(10L);
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse response = userService.removeAvatar(userId);

        assertThat(response.avatarUrl()).isNull();
        assertThat(testUser.getAvatarStorageKey()).isNull();
        assertThat(testUser.getAvatarContentType()).isNull();
        assertThat(testUser.getAvatarSizeBytes()).isNull();
        verify(storageService).deleteObject("avatars/%s/avatar.png".formatted(userId));
        verify(userRepository).save(testUser);
    }
}
