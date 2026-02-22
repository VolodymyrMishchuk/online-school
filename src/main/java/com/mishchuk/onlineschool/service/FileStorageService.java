package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.config.MinioConfig;
import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.exception.InvalidFileTypeException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.FileMapper;
import com.mishchuk.onlineschool.repository.FileRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.entity.FileEntity;
import com.mishchuk.onlineschool.repository.entity.LessonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.context.SecurityContextHolder;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    // Allowed MIME types for file uploads
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf", // PDF
            "image/jpeg", // JPEG
            "image/jpg", // JPG (alternative MIME type)
            "image/png" // PNG
    );

    private final MinioService minioService;
    private final FileRepository fileRepository;
    private final LessonRepository lessonRepository;
    private final MinioConfig minioConfig;
    private final FileMapper fileMapper;
    private final PersonRepository personRepository;
    private final EnrollmentRepository enrollmentRepository;

    @Transactional
    public FileDto uploadFile(
            MultipartFile file,
            String entityType,
            UUID entityId,
            PersonEntity uploadedBy) {
        try {
            // Validate MIME type
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
                throw new InvalidFileTypeException(
                        "Invalid file type. Only PDF, JPEG, and PNG files are allowed. Received: " +
                                (contentType != null ? contentType : "unknown"));
            }

            // Upload to MinIO
            String folder = entityType != null ? entityType.toLowerCase() : "general";
            String minioObjectName = minioService.uploadFile(file, folder);

            // Save metadata to DB
            FileEntity fileEntity = new FileEntity();
            fileEntity.setFileName(file.getOriginalFilename());
            fileEntity.setOriginalName(file.getOriginalFilename());
            fileEntity.setContentType(file.getContentType());
            fileEntity.setFileSize(file.getSize());
            fileEntity.setMinioObjectName(minioObjectName);
            fileEntity.setBucketName(minioConfig.getBucketName());
            fileEntity.setUploadedBy(uploadedBy);
            fileEntity.setRelatedEntityType(entityType);
            fileEntity.setRelatedEntityId(entityId);

            // Встановити lesson relationship якщо entityType = LESSON
            if ("LESSON".equalsIgnoreCase(entityType) && entityId != null) {
                LessonEntity lesson = lessonRepository.findById(entityId)
                        .orElseThrow(() -> new ResourceNotFoundException("Lesson not found: " + entityId));
                fileEntity.setLesson(lesson);
            }

            FileEntity saved = fileRepository.save(fileEntity);
            log.info("File uploaded successfully: {}", saved.getId());

            return fileMapper.toDto(saved);
        } catch (Exception e) {
            log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public FileDownloadDto downloadFile(UUID fileId) {
        try {
            FileEntity fileEntity = fileRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

            // Verify access if this file belongs to a lesson
            if ("LESSON".equalsIgnoreCase(fileEntity.getRelatedEntityType()) && fileEntity.getLesson() != null) {
                if (!hasAccessToLessonFiles(fileEntity.getLesson().getId())) {
                    throw new RuntimeException(
                            "Access denied: You do not have permission to download files for this lesson.");
                }
            }

            InputStream fileStream = minioService.downloadFile(fileEntity.getMinioObjectName());

            return new FileDownloadDto(fileStream, fileEntity);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to download file: {}", fileId, e);
            throw new RuntimeException("Failed to download file", e);
        }
    }

    @Transactional
    public void deleteFile(UUID fileId) {
        try {
            FileEntity fileEntity = fileRepository.findById(fileId)
                    .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));

            // Delete from MinIO
            minioService.deleteFile(fileEntity.getMinioObjectName());

            // Delete metadata from DB
            fileRepository.delete(fileEntity);

            log.info("File deleted successfully: {}", fileId);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to delete file: {}", fileId, e);
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public List<FileDto> getFilesForEntity(String entityType, UUID entityId) {
        List<FileEntity> files = fileRepository.findByRelatedEntityTypeAndRelatedEntityId(entityType, entityId);
        return files.stream()
                .map(fileMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FileDto> getFilesByUser(PersonEntity person) {
        List<FileEntity> files = fileRepository.findByUploadedBy(person);
        return files.stream()
                .map(fileMapper::toDto)
                .collect(Collectors.toList());
    }

    // Методи для роботи з файлами уроків
    public List<FileDto> getLessonFiles(UUID lessonId) {
        if (!hasAccessToLessonFiles(lessonId)) {
            return List.of(); // Return empty list to avoid leaking information
        }

        List<FileEntity> files = fileRepository.findByLessonId(lessonId);
        return files.stream()
                .map(fileMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<FileDto> getLessonFilesOrdered(UUID lessonId) {
        if (!hasAccessToLessonFiles(lessonId)) {
            return List.of();
        }

        List<FileEntity> files = fileRepository.findLessonFilesOrdered(lessonId);
        return files.stream()
                .map(fileMapper::toDto)
                .collect(Collectors.toList());
    }

    private boolean hasAccessToLessonFiles(UUID lessonId) {
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Optional<PersonEntity> userOpt = personRepository.findByEmail(userEmail);

        if (userOpt.isPresent()) {
            PersonEntity user = userOpt.get();
            if (user.getRole() == PersonRole.ADMIN || user.getRole() == PersonRole.FAKE_ADMIN) {
                return true;
            }

            LessonEntity lesson = lessonRepository.findById(lessonId).orElse(null);
            if (lesson != null && lesson.getModule() != null && lesson.getModule().getCourse() != null) {
                Optional<EnrollmentEntity> enrollmentOpt = enrollmentRepository
                        .findByStudentIdAndCourseId(user.getId(), lesson.getModule().getCourse().getId());

                if (enrollmentOpt.isPresent()) {
                    EnrollmentEntity enrollment = enrollmentOpt.get();

                    if ("BLOCKED".equals(enrollment.getStatus())) {
                        return false;
                    }

                    if (enrollment.getExpiresAt() != null) {
                        if (OffsetDateTime.now().isAfter(enrollment.getExpiresAt())) {
                            return false;
                        }
                    } else if (lesson.getModule().getCourse().getAccessDuration() != null) {
                        OffsetDateTime expirationDate = enrollment.getCreatedAt()
                                .plusDays(lesson.getModule().getCourse().getAccessDuration());
                        if (OffsetDateTime.now().isAfter(expirationDate)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }

    // Inner class for download response
    public record FileDownloadDto(InputStream inputStream, FileEntity metadata) {
    }
}
