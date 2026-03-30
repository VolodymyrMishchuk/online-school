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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        userId = UUID.randomUUID();
        user = new PersonEntity();
        user.setId(userId);
        user.setFirstName("Тест");
        user.setLastName("Юзер");
        user.setEmail("test@example.com");
    }

    // --- createAppeal ---

    @Test
    @DisplayName("createAppeal — успішне збереження та сповіщення адмінів")
    void createAppeal_success() {
        AppealCreateRequest request = new AppealCreateRequest();
        request.setContactMethod(ContactMethod.MOBILE);
        request.setContactDetails("+380991234567");
        request.setMessage("Тест");

        AppealEntity saved = new AppealEntity();
        saved.setId(UUID.randomUUID());
        saved.setStatus(AppealStatus.NEW);
        saved.setUser(user);

        AppealResponse response = new AppealResponse();

        when(personRepository.findById(userId)).thenReturn(Optional.of(user));
        when(appealRepository.save(any(AppealEntity.class))).thenReturn(saved);
        when(appealMapper.toDto(saved)).thenReturn(response);
        when(fileStorageService.getFilesForEntity(eq("APPEAL"), any())).thenReturn(Collections.emptyList());

        AppealResponse result = appealService.createAppeal(userId, request, Collections.emptyList());

        assertThat(result).isNotNull();
        verify(appealRepository).save(any(AppealEntity.class));
        verify(notificationService).broadcastToAdmins(
                anyString(), anyString(), eq(NotificationType.NEW_APPEAL), anyString()
        );
    }

    @Test
    @DisplayName("createAppeal — кидає ResourceNotFoundException якщо користувача не знайдено")
    void createAppeal_userNotFound_throws() {
        when(personRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                appealService.createAppeal(userId, new AppealCreateRequest(), Collections.emptyList())
        ).isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(userId.toString());
    }

    // --- createPublicAppeal ---

    @Test
    @DisplayName("createPublicAppeal — успішне збереження з guestName")
    void createPublicAppeal_success() {
        PublicAppealCreateRequest request = new PublicAppealCreateRequest();
        request.setName("Гість");
        request.setContactMethod(ContactMethod.EMAIL);
        request.setContactDetails("guest@example.com");
        request.setMessage("Питання");

        AppealEntity saved = new AppealEntity();
        saved.setId(UUID.randomUUID());
        saved.setGuestName("Гість");

        AppealResponse response = new AppealResponse();

        when(appealRepository.save(any(AppealEntity.class))).thenReturn(saved);
        when(appealMapper.toDto(saved)).thenReturn(response);
        when(fileStorageService.getFilesForEntity(eq("APPEAL"), any())).thenReturn(Collections.emptyList());

        AppealResponse result = appealService.createPublicAppeal(request, Collections.emptyList());

        assertThat(result).isNotNull();

        ArgumentCaptor<AppealEntity> captor = ArgumentCaptor.forClass(AppealEntity.class);
        verify(appealRepository).save(captor.capture());
        assertThat(captor.getValue().getGuestName()).isEqualTo("Гість");
        assertThat(captor.getValue().getUser()).isNull();

        verify(notificationService).broadcastToAdmins(
                anyString(), anyString(), eq(NotificationType.NEW_APPEAL), anyString()
        );
    }

    // --- getAppeal ---

    @Test
    @DisplayName("getAppeal — повертає DTO якщо знайдено")
    void getAppeal_found() {
        UUID id = UUID.randomUUID();
        AppealEntity entity = new AppealEntity();
        entity.setId(id);
        AppealResponse response = new AppealResponse();

        when(appealRepository.findById(id)).thenReturn(Optional.of(entity));
        when(appealMapper.toDto(entity)).thenReturn(response);
        when(fileStorageService.getFilesForEntity(eq("APPEAL"), eq(id))).thenReturn(Collections.emptyList());

        AppealResponse result = appealService.getAppeal(id);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("getAppeal — кидає ResourceNotFoundException якщо не знайдено")
    void getAppeal_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(appealRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appealService.getAppeal(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- updateAppealStatus ---

    @Test
    @DisplayName("updateAppealStatus — оновлює статус і зберігає")
    void updateAppealStatus_success() {
        UUID id = UUID.randomUUID();
        AppealEntity entity = new AppealEntity();
        entity.setId(id);
        entity.setStatus(AppealStatus.NEW);
        AppealResponse response = new AppealResponse();

        when(appealRepository.findById(id)).thenReturn(Optional.of(entity));
        when(appealRepository.save(entity)).thenReturn(entity);
        when(appealMapper.toDto(entity)).thenReturn(response);
        when(fileStorageService.getFilesForEntity(eq("APPEAL"), eq(id))).thenReturn(Collections.emptyList());

        appealService.updateAppealStatus(id, AppealStatus.PROCESSED);

        assertThat(entity.getStatus()).isEqualTo(AppealStatus.PROCESSED);
        verify(appealRepository).save(entity);
    }

    @Test
    @DisplayName("updateAppealStatus — кидає ResourceNotFoundException якщо не знайдено")
    void updateAppealStatus_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(appealRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appealService.updateAppealStatus(id, AppealStatus.PROCESSED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- deleteAppeal ---

    @Test
    @DisplayName("deleteAppeal — видаляє файли та ентіті")
    void deleteAppeal_deletesFilesAndEntity() {
        UUID id = UUID.randomUUID();
        AppealEntity entity = new AppealEntity();
        entity.setId(id);

        FileDto fileDto = new FileDto();
        fileDto.setId(UUID.randomUUID());

        when(appealRepository.findById(id)).thenReturn(Optional.of(entity));
        when(fileStorageService.getFilesForEntity("APPEAL", id)).thenReturn(List.of(fileDto));

        appealService.deleteAppeal(id);

        verify(fileStorageService).deleteFile(fileDto.getId());
        verify(appealRepository).delete(entity);
    }
}
