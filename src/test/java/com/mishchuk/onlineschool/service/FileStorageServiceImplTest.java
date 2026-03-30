package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.config.MinioConfig;
import com.mishchuk.onlineschool.exception.InvalidFileTypeException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.FileMapper;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.FileRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.FileEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    @Mock private MinioService minioService;
    @Mock private FileRepository fileRepository;
    @Mock private LessonRepository lessonRepository;
    @Mock private FileMapper fileMapper;
    @Mock private PersonRepository personRepository;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private PersonEntity adminUser;

    @BeforeEach
    void setUp() {
        adminUser = new PersonEntity();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

        // MinioConfig is a @Data POJO — inject directly without mocking
        MinioConfig minioConfig = new MinioConfig();
        minioConfig.setBucketName("test-bucket");
        ReflectionTestUtils.setField(fileStorageService, "minioConfig", minioConfig);

        var auth = new UsernamePasswordAuthenticationToken("admin@test.com", null);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
        lenient().when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
    }

    // --- uploadFile ---

    @Test
    @DisplayName("uploadFile — кидає InvalidFileTypeException для недозволеного MIME типу")
    void uploadFile_invalidMimeType_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.js", "text/javascript", "code".getBytes());

        assertThatThrownBy(() -> fileStorageService.uploadFile(file, "LESSON", UUID.randomUUID(), adminUser))
                .isInstanceOf(RuntimeException.class)
                .cause().isInstanceOf(com.mishchuk.onlineschool.exception.InvalidFileTypeException.class);
    }

    @Test
    @DisplayName("uploadFile — зберігає PDF файл та метадані")
    void uploadFile_pdfFile_savesMetadata() throws Exception {
        UUID lessonId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "data".getBytes());

        when(minioService.uploadFile(any(), anyString())).thenReturn("lesson/uuid_notes.pdf");
        when(lessonRepository.findById(lessonId))
                .thenReturn(Optional.of(new com.mishchuk.onlineschool.repository.entity.LessonEntity()));

        FileEntity saved = new FileEntity();
        when(fileRepository.save(any())).thenReturn(saved);
        when(fileMapper.toDto(saved)).thenReturn(null);

        fileStorageService.uploadFile(file, "LESSON", lessonId, adminUser);

        verify(fileRepository).save(any(FileEntity.class));
    }

    // --- deleteFile ---

    @Test
    @DisplayName("deleteFile — кидає ResourceNotFoundException якщо файл не знайдено")
    void deleteFile_notFound_throws() {
        UUID fileId = UUID.randomUUID();
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileStorageService.deleteFile(fileId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("deleteFile — видаляє файл з MinIO та DB")
    void deleteFile_success() throws Exception {
        UUID fileId = UUID.randomUUID();
        FileEntity entity = new FileEntity();
        entity.setMinioObjectName("lessons/file.pdf");

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(entity));

        fileStorageService.deleteFile(fileId);

        verify(minioService).deleteFile("lessons/file.pdf");
        verify(fileRepository).delete(entity);
    }

    // --- getLessonFiles ---

    @Test
    @DisplayName("getLessonFiles — ADMIN отримує список файлів (порожній у тесті)")
    void getLessonFiles_admin_returnsFiles() {
        UUID lessonId = UUID.randomUUID();
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(fileRepository.findByLessonId(lessonId)).thenReturn(List.of());

        List<?> result = fileStorageService.getLessonFiles(lessonId);

        assertThat(result).isNotNull();
    }
}
