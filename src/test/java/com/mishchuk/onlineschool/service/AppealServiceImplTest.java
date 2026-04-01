package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.AppealCreateRequest;
import com.mishchuk.onlineschool.controller.dto.AppealResponse;
import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.controller.dto.PublicAppealCreateRequest;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.AppealMapper;
import com.mishchuk.onlineschool.repository.AppealRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.AppealEntity;
import com.mishchuk.onlineschool.repository.entity.AppealStatus;
import com.mishchuk.onlineschool.repository.entity.ContactMethod;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppealServiceImplTest {

    @Mock private AppealRepository appealRepository;
    @Mock private PersonRepository personRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private NotificationService notificationService;
    @Mock private AppealMapper appealMapper;

    @InjectMocks
    private AppealServiceImpl appealService;

    private UUID userId;
    private PersonEntity user;

    @BeforeEach
    void setUp() {
        userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        user = new PersonEntity();
        user.setId(userId);
        user.setFirstName("Тест");
        user.setLastName("Юзер");
        user.setEmail("test@example.com");

        lenient().when(fileStorageService.getFilesForEntity(any(), any()))
                .thenReturn(Collections.emptyList());
    }

    // ─────────────────────── createAppeal ───────────────────────

    @Test
    @DisplayName("createAppeal — зберігає entity з коректними полями та повертає саме той об'єкт з mapper")
    void createAppeal_savesEntityWithCorrectFields() {
        AppealCreateRequest request = appealCreateRequest();
        AppealEntity saved = savedAppealEntity(userId);
        AppealResponse expectedResponse = new AppealResponse();

        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(saved);
        when(appealMapper.toDto(saved)).thenReturn(expectedResponse);

        AppealResponse result = appealService.createAppeal(userId, request, Collections.emptyList());

        ArgumentCaptor<AppealEntity> captor = ArgumentCaptor.forClass(AppealEntity.class);
        verify(appealRepository).save(captor.capture());

        AppealEntity captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(user);
        assertThat(captured.getContactMethod()).isEqualTo(ContactMethod.EMAIL);
        assertThat(captured.getContactDetails()).isEqualTo("user@test.com");
        assertThat(captured.getMessage()).isEqualTo("Текст звернення");
        assertThat(captured.getStatus()).isEqualTo(AppealStatus.NEW);

        verify(appealMapper).toDto(saved);
        // [5] Перевіряємо що повертається саме той об'єкт, не новий
        assertThat(result).isSameAs(expectedResponse);
    }

    @Test
    @DisplayName("createAppeal — сповіщає адмінів з ім'ям користувача та NEW_APPEAL")
    void createAppeal_notifiesAdminsWithUserNameAndCorrectType() {
        AppealEntity saved = savedAppealEntity(userId);
        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(saved);
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        appealService.createAppeal(userId, appealCreateRequest(), Collections.emptyList());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).broadcastToAdmins(
                anyString(),
                messageCaptor.capture(),
                eq(NotificationType.NEW_APPEAL),
                urlCaptor.capture()
        );
        // [6] Точна перевірка тексту повідомлення
        assertThat(messageCaptor.getValue()).isEqualTo("Отримано нове звернення від Тест Юзер");
        assertThat(urlCaptor.getValue()).contains(saved.getId().toString());
    }

    @Test
    @DisplayName("createAppeal — коли firstName/lastName null, використовує email у повідомленні")
    void createAppeal_noName_usesEmailInNotification() {
        user.setFirstName(null);
        user.setLastName(null);

        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(savedAppealEntity(userId));
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        appealService.createAppeal(userId, appealCreateRequest(), Collections.emptyList());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).broadcastToAdmins(
                anyString(), messageCaptor.capture(), any(), anyString());
        assertThat(messageCaptor.getValue()).contains("test@example.com");
    }

    @Test
    @DisplayName("createAppeal — тільки firstName без lastName → використовує email")
    void createAppeal_onlyFirstName_usesEmail() {
        user.setFirstName("Тест");
        user.setLastName(null);

        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(savedAppealEntity(userId));
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        appealService.createAppeal(userId, appealCreateRequest(), Collections.emptyList());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).broadcastToAdmins(
                anyString(), messageCaptor.capture(), any(), anyString());
        assertThat(messageCaptor.getValue()).contains("test@example.com");
    }

    @Test
    @DisplayName("createAppeal — тільки lastName без firstName → використовує email")
    void createAppeal_onlyLastName_usesEmail() {
        user.setFirstName(null);
        user.setLastName("Юзер");

        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(savedAppealEntity(userId));
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        appealService.createAppeal(userId, appealCreateRequest(), Collections.emptyList());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).broadcastToAdmins(
                anyString(), messageCaptor.capture(), any(), anyString());
        assertThat(messageCaptor.getValue()).contains("test@example.com");
    }

    @Test
    @DisplayName("createAppeal — завантажує фото через fileStorageService")
    void createAppeal_uploadsPhotos() {
        AppealEntity saved = savedAppealEntity(userId);
        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(saved);
        when(appealMapper.toDto(saved)).thenReturn(new AppealResponse());

        MultipartFile photo1 = new MockMultipartFile("photo1.jpg", "photo1.jpg", "image/jpeg", "img1".getBytes());
        MultipartFile photo2 = new MockMultipartFile("photo2.jpg", "photo2.jpg", "image/jpeg", "img2".getBytes());

        appealService.createAppeal(userId, appealCreateRequest(), List.of(photo1, photo2));

        ArgumentCaptor<MultipartFile> fileCaptor = ArgumentCaptor.forClass(MultipartFile.class);
        verify(fileStorageService, times(2))
                .uploadFile(fileCaptor.capture(), eq("APPEAL"), eq(saved.getId()), eq(user));
        assertThat(fileCaptor.getAllValues())
                .extracting(MultipartFile::getOriginalFilename)
                .containsExactlyInAnyOrder("photo1.jpg", "photo2.jpg");
    }

    @Test
    @DisplayName("createAppeal — порожні фото не завантажуються")
    void createAppeal_emptyPhotos_skipsUpload() {
        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(savedAppealEntity(userId));
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        MultipartFile emptyPhoto = new MockMultipartFile("empty.jpg", new byte[0]);

        appealService.createAppeal(userId, appealCreateRequest(), List.of(emptyPhoto));

        verify(fileStorageService, never()).uploadFile(any(), any(), any(), any());
    }

    // [5] Тест: photos == null не крашить і не викликає uploadFile
    @Test
    @DisplayName("createAppeal — null photos не призводить до помилки")
    void createAppeal_nullPhotos_doesNotCrash() {
        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any())).thenReturn(savedAppealEntity(userId));
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        appealService.createAppeal(userId, appealCreateRequest(), null);

        verify(fileStorageService, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("createAppeal — кидає ResourceNotFoundException якщо користувача не знайдено")
    void createAppeal_userNotFound_throws() {
        when(personRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                appealService.createAppeal(userId, appealCreateRequest(), Collections.emptyList())
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());

        verify(appealRepository, never()).save(any());
        verify(notificationService, never()).broadcastToAdmins(any(), any(), any(), any());
        verify(fileStorageService, never()).uploadFile(any(), any(), any(), any());
        verify(appealMapper, never()).toDto(any());
    }

    // ─────────────────────── createPublicAppeal ───────────────────────

    @Test
    @DisplayName("createPublicAppeal — зберігає entity з guestName і без user")
    void createPublicAppeal_savesEntityWithGuestNameAndNoUser() {
        PublicAppealCreateRequest request = publicAppealCreateRequest();
        AppealEntity saved = savedPublicAppealEntity();

        when(appealRepository.save(any())).thenReturn(saved);
        when(appealMapper.toDto(saved)).thenReturn(new AppealResponse());

        appealService.createPublicAppeal(request, Collections.emptyList());

        ArgumentCaptor<AppealEntity> captor = ArgumentCaptor.forClass(AppealEntity.class);
        verify(appealRepository).save(captor.capture());

        AppealEntity captured = captor.getValue();
        assertThat(captured.getGuestName()).isEqualTo("Гість");
        assertThat(captured.getUser()).isNull();
        assertThat(captured.getContactMethod()).isEqualTo(ContactMethod.TELEGRAM);
        assertThat(captured.getContactDetails()).isEqualTo("@guest_tg");
        assertThat(captured.getMessage()).isEqualTo("Публічне звернення");
        assertThat(captured.getStatus()).isEqualTo(AppealStatus.NEW);
        verify(appealMapper).toDto(saved);
    }

    @Test
    @DisplayName("createPublicAppeal — сповіщає адмінів з ім'ям гостя та NEW_APPEAL")
    void createPublicAppeal_notifiesAdminsWithGuestNameAndCorrectType() {
        AppealEntity saved = savedPublicAppealEntity();
        when(appealRepository.save(any())).thenReturn(saved);
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        appealService.createPublicAppeal(publicAppealCreateRequest(), Collections.emptyList());

        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).broadcastToAdmins(
                anyString(),
                messageCaptor.capture(),
                eq(NotificationType.NEW_APPEAL),
                urlCaptor.capture()
        );
        assertThat(messageCaptor.getValue()).contains("Гість");
        assertThat(urlCaptor.getValue()).contains(saved.getId().toString());
    }

    @Test
    @DisplayName("createPublicAppeal — завантажує фото з null як user")
    void createPublicAppeal_uploadsPhotosWithNullUser() {
        AppealEntity saved = savedPublicAppealEntity();
        when(appealRepository.save(any())).thenReturn(saved);
        when(appealMapper.toDto(saved)).thenReturn(new AppealResponse());

        MultipartFile photo = new MockMultipartFile("photo.jpg", "photo.jpg", "image/jpeg", "img".getBytes());

        appealService.createPublicAppeal(publicAppealCreateRequest(), List.of(photo));

        verify(fileStorageService, times(1)).uploadFile(any(), eq("APPEAL"), eq(saved.getId()), isNull());
    }

    @Test
    @DisplayName("createPublicAppeal — порожні фото не завантажуються")
    void createPublicAppeal_emptyPhotos_skipsUpload() {
        when(appealRepository.save(any())).thenReturn(savedPublicAppealEntity());
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        MultipartFile emptyPhoto = new MockMultipartFile("empty.jpg", new byte[0]);

        appealService.createPublicAppeal(publicAppealCreateRequest(), List.of(emptyPhoto));

        verify(fileStorageService, never()).uploadFile(any(), any(), any(), any());
    }

    @Test
    @DisplayName("createPublicAppeal — null photos не призводить до помилки")
    void createPublicAppeal_nullPhotos_doesNotCrash() {
        when(appealRepository.save(any())).thenReturn(savedPublicAppealEntity());
        when(appealMapper.toDto(any())).thenReturn(new AppealResponse());

        appealService.createPublicAppeal(publicAppealCreateRequest(), null);

        verify(fileStorageService, never()).uploadFile(any(), any(), any(), any());
    }

    // ─────────────────────── getAppeals ───────────────────────

    @Test
    @DisplayName("getAppeals — повертає сторінку AppealResponse")
    void getAppeals_returnsPage() {
        AppealEntity entity = new AppealEntity();
        entity.setId(UUID.randomUUID());
        AppealResponse response = new AppealResponse();
        PageRequest pageable = PageRequest.of(0, 20);

        when(appealRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(appealMapper.toDto(entity)).thenReturn(response);

        var result = appealService.getAppeals(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);

        verify(appealMapper).toDto(entity);
        verify(fileStorageService).getFilesForEntity("APPEAL", entity.getId());
    }

    @Test
    @DisplayName("getAppeals — порожня сторінка повертається коректно")
    void getAppeals_emptyPage_returnsEmpty() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(appealRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        var result = appealService.getAppeals(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verify(appealMapper, never()).toDto(any());
        verify(fileStorageService, never()).getFilesForEntity(any(), any());
    }

    // ─────────────────────── getAppeal ───────────────────────

    @Test
    @DisplayName("getAppeal — повертає DTO з фото якщо знайдено")
    void getAppeal_found_returnsDtoWithPhotos() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        AppealEntity entity = new AppealEntity();
        entity.setId(id);
        AppealResponse response = new AppealResponse();
        FileDto photo = new FileDto();
        photo.setId(UUID.randomUUID());

        when(appealRepository.findById(id)).thenReturn(Optional.of(entity));
        when(appealMapper.toDto(entity)).thenReturn(response);
        when(fileStorageService.getFilesForEntity("APPEAL", id)).thenReturn(List.of(photo));

        AppealResponse result = appealService.getAppeal(id);

        assertThat(result).isNotNull();
        assertThat(result.getPhotos()).hasSize(1);
    }

    @Test
    @DisplayName("getAppeal — кидає ResourceNotFoundException якщо не знайдено")
    void getAppeal_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(appealRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appealService.getAppeal(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─────────────────────── updateAppealStatus ───────────────────────

    @ParameterizedTest
    @EnumSource(AppealStatus.class)
    @DisplayName("updateAppealStatus — оновлює статус для кожного значення AppealStatus")
    void updateAppealStatus_updatesForAllStatuses(AppealStatus status) {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        AppealEntity entity = new AppealEntity();
        entity.setId(id);
        entity.setStatus(AppealStatus.NEW);

        when(appealRepository.findById(id)).thenReturn(Optional.of(entity));
        when(appealRepository.save(entity)).thenReturn(entity);
        when(appealMapper.toDto(entity)).thenReturn(new AppealResponse());

        appealService.updateAppealStatus(id, status);

        assertThat(entity.getStatus()).isEqualTo(status);
        verify(appealRepository).save(entity);
        verify(appealMapper).toDto(entity);
        verify(fileStorageService).getFilesForEntity("APPEAL", id);
    }

    @Test
    @DisplayName("updateAppealStatus — кидає ResourceNotFoundException якщо не знайдено")
    void updateAppealStatus_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(appealRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appealService.updateAppealStatus(id, AppealStatus.PROCESSED))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    // ─────────────────────── deleteAppeal ───────────────────────

    @Test
    @DisplayName("deleteAppeal — видаляє всі фото та ентіті")
    void deleteAppeal_deletesPhotosAndEntity() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        AppealEntity entity = new AppealEntity();
        entity.setId(id);

        UUID photoId1 = UUID.fromString("00000000-0000-0000-0000-000000000011");
        UUID photoId2 = UUID.fromString("00000000-0000-0000-0000-000000000012");
        FileDto photo1 = new FileDto(); photo1.setId(photoId1);
        FileDto photo2 = new FileDto(); photo2.setId(photoId2);

        when(appealRepository.findById(id)).thenReturn(Optional.of(entity));
        when(fileStorageService.getFilesForEntity("APPEAL", id)).thenReturn(List.of(photo1, photo2));

        appealService.deleteAppeal(id);

        verify(fileStorageService).deleteFile(photoId1);
        verify(fileStorageService).deleteFile(photoId2);
        verify(appealRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteAppeal — без фото видаляє лише ентіті")
    void deleteAppeal_noPhotos_deletesOnlyEntity() {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        AppealEntity entity = new AppealEntity();
        entity.setId(id);

        when(appealRepository.findById(id)).thenReturn(Optional.of(entity));
        when(fileStorageService.getFilesForEntity("APPEAL", id)).thenReturn(Collections.emptyList());

        appealService.deleteAppeal(id);

        verify(fileStorageService, never()).deleteFile(any());
        verify(appealRepository).delete(entity);
    }

    @Test
    @DisplayName("deleteAppeal — кидає ResourceNotFoundException якщо не знайдено")
    void deleteAppeal_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(appealRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appealService.deleteAppeal(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(fileStorageService, never()).getFilesForEntity(any(), any());
        verify(fileStorageService, never()).deleteFile(any());
        verify(appealRepository, never()).delete(any());
    }

    // ─────────────────────── helpers ───────────────────────

    private AppealCreateRequest appealCreateRequest() {
        AppealCreateRequest request = new AppealCreateRequest();
        request.setContactMethod(ContactMethod.EMAIL);
        request.setContactDetails("user@test.com");
        request.setMessage("Текст звернення");
        return request;
    }

    private PublicAppealCreateRequest publicAppealCreateRequest() {
        PublicAppealCreateRequest request = new PublicAppealCreateRequest();
        request.setName("Гість");
        request.setContactMethod(ContactMethod.TELEGRAM);
        request.setContactDetails("@guest_tg");
        request.setMessage("Публічне звернення");
        return request;
    }

    private AppealEntity savedAppealEntity(UUID userId) {
        AppealEntity entity = new AppealEntity();
        entity.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        entity.setStatus(AppealStatus.NEW);
        entity.setUser(user);
        return entity;
    }

    private AppealEntity savedPublicAppealEntity() {
        AppealEntity entity = new AppealEntity();
        entity.setId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        entity.setGuestName("Гість");
        entity.setStatus(AppealStatus.NEW);
        return entity;
    }
}