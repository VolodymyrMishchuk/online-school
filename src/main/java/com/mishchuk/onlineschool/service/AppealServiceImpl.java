package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.AppealCreateRequest;
import com.mishchuk.onlineschool.controller.dto.PublicAppealCreateRequest;
import com.mishchuk.onlineschool.controller.dto.AppealResponse;
import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.AppealMapper;
import com.mishchuk.onlineschool.repository.AppealRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.AppealEntity;
import com.mishchuk.onlineschool.repository.entity.AppealStatus;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppealServiceImpl implements AppealService {

    private final AppealRepository appealRepository;
    private final PersonRepository personRepository;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;
    private final AppealMapper appealMapper;

    @Override
    @Transactional
    public AppealResponse createAppeal(UUID userId, AppealCreateRequest request, List<MultipartFile> photos) {
        PersonEntity user = personRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        AppealEntity appeal = new AppealEntity();
        appeal.setUser(user);
        appeal.setContactMethod(request.getContactMethod());
        appeal.setContactDetails(request.getContactDetails());
        appeal.setMessage(request.getMessage());
        appeal.setStatus(AppealStatus.NEW);

        AppealEntity savedAppeal = appealRepository.save(appeal);

        // Process photos
        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile photo : photos) {
                if (!photo.isEmpty()) {
                    fileStorageService.uploadFile(photo, "APPEAL", savedAppeal.getId(), user);
                }
            }
        }

        // Notify admins
        String userName = (user.getFirstName() != null && user.getLastName() != null)
                ? user.getFirstName() + " " + user.getLastName()
                : user.getEmail();
        String title = "Нове звернення";
        String messageStr = "Отримано нове звернення від " + userName;
        String buttonUrl = "/dashboard/appeals?id=" + savedAppeal.getId();

        notificationService.broadcastToAdmins(title, messageStr, NotificationType.NEW_APPEAL, buttonUrl);

        return enrichWithPhotos(savedAppeal);
    }

    @Override
    @Transactional
    public AppealResponse createPublicAppeal(PublicAppealCreateRequest request, List<MultipartFile> photos) {
        AppealEntity appeal = new AppealEntity();
        appeal.setGuestName(request.getName());
        appeal.setContactMethod(request.getContactMethod());
        appeal.setContactDetails(request.getContactDetails());
        appeal.setMessage(request.getMessage());
        appeal.setStatus(AppealStatus.NEW);

        AppealEntity savedAppeal = appealRepository.save(appeal);

        // Process photos
        if (photos != null && !photos.isEmpty()) {
            for (MultipartFile photo : photos) {
                if (!photo.isEmpty()) {
                    fileStorageService.uploadFile(photo, "APPEAL", savedAppeal.getId(), null);
                }
            }
        }

        // Notify admins
        String title = "Нове звернення з лендінгу";
        String messageStr = "Отримано нове зовнішнє звернення від " + request.getName();
        String buttonUrl = "/dashboard/appeals?id=" + savedAppeal.getId();

        notificationService.broadcastToAdmins(title, messageStr, NotificationType.NEW_APPEAL, buttonUrl);

        return enrichWithPhotos(savedAppeal);
    }

    @Override
    public Page<AppealResponse> getAppeals(Pageable pageable) {
        return appealRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::enrichWithPhotos);
    }

    @Override
    public AppealResponse getAppeal(UUID id) {
        AppealEntity appeal = appealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal not found: " + id));
        return enrichWithPhotos(appeal);
    }

    @Override
    @Transactional
    public AppealResponse updateAppealStatus(UUID id, AppealStatus status) {
        AppealEntity appeal = appealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal not found: " + id));

        appeal.setStatus(status);
        AppealEntity saved = appealRepository.save(appeal);

        return enrichWithPhotos(saved);
    }

    @Override
    @Transactional
    public void deleteAppeal(UUID id) {
        AppealEntity appeal = appealRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appeal not found: " + id));

        // Delete associated files
        List<FileDto> photos = fileStorageService.getFilesForEntity("APPEAL", id);
        for (FileDto photo : photos) {
            fileStorageService.deleteFile(photo.getId());
        }

        appealRepository.delete(appeal);
    }

    private AppealResponse enrichWithPhotos(AppealEntity entity) {
        AppealResponse response = appealMapper.toDto(entity);
        List<FileDto> photos = fileStorageService.getFilesForEntity("APPEAL", entity.getId());
        response.setPhotos(photos);
        return response;
    }
}
