package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.config.MinioConfig;
import com.mishchuk.onlineschool.controller.dto.FileDto;
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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
        adminUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

        // MinioConfig — @Data POJO, ін'єктуємо напряму
        MinioConfig minioConfig = new MinioConfig();
        minioConfig.setBucketName("test-bucket");
        ReflectionTestUtils.setField(fileStorageService, "minioConfig", minioConfig);

        // 3-аргументний конструктор = isAuthenticated() = true
        setSecurityContext("admin@test.com", "ROLE_ADMIN");
        lenient().when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        
        ((Logger) LoggerFactory.getLogger(FileStorageServiceImpl.class)).setLevel(Level.OFF);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
        ((Logger) LoggerFactory.getLogger(FileStorageServiceImpl.class)).setLevel(null);
    }

    private void setSecurityContext(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(
                email, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // ─────────────────────── uploadFile ───────────────────────

    @Test
    @DisplayName("uploadFile — кидає InvalidFileTypeException для недозволеного MIME типу")
    void uploadFile_invalidMimeType_throws() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "script.js", "text/javascript", "code".getBytes());

        assertThatThrownBy(() -> fileStorageService.uploadFile(file, "LESSON", UUID.randomUUID(), adminUser))
                .isInstanceOf(RuntimeException.class)
                .cause().isInstanceOf(InvalidFileTypeException.class);

        // MinIO і DB не торкались
        verify(minioService, never()).uploadFile(any(), anyString());
        verify(minioService, never()).deleteFile(anyString());
        verify(fileRepository, never()).save(any());
    }

    @Test
    @DisplayName("uploadFile — кидає InvalidFileTypeException для null contentType")
    void uploadFile_nullContentType_throws() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "file.pdf", null, "data".getBytes());

        assertThatThrownBy(() -> fileStorageService.uploadFile(file, "LESSON", UUID.randomUUID(), adminUser))
                .isInstanceOf(RuntimeException.class)
                .cause().isInstanceOf(InvalidFileTypeException.class);

        verify(minioService, never()).uploadFile(any(), anyString());
        verify(minioService, never()).deleteFile(anyString());
    }

    @Test
    @DisplayName("uploadFile — зберігає PDF файл та встановлює метадані в entity")
    void uploadFile_pdfFile_savesMetadataCorrectly() throws Exception {
        UUID lessonId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        MockMultipartFile file = new MockMultipartFile(
                "file", "notes.pdf", "application/pdf", "data".getBytes());

        when(minioService.uploadFile(any(), anyString())).thenReturn("lesson/uuid_notes.pdf");
        when(lessonRepository.findById(lessonId))
                .thenReturn(Optional.of(new com.mishchuk.onlineschool.repository.entity.LessonEntity()));
        FileEntity saved = new FileEntity();
        when(fileRepository.save(any())).thenReturn(saved);
        when(fileMapper.toDto(saved)).thenReturn(new FileDto(null, null, null, null, null, null, null, null, null));

        fileStorageService.uploadFile(file, "LESSON", lessonId, adminUser);

        ArgumentCaptor<FileEntity> captor = ArgumentCaptor.forClass(FileEntity.class);
        verify(fileRepository).save(captor.capture());

        FileEntity entity = captor.getValue();
        assertThat(entity.getFileName()).isEqualTo("notes.pdf");
        assertThat(entity.getContentType()).isEqualTo("application/pdf");
        assertThat(entity.getMinioObjectName()).isEqualTo("lesson/uuid_notes.pdf");
        assertThat(entity.getBucketName()).isEqualTo("test-bucket");
        assertThat(entity.getUploadedBy()).isSameAs(adminUser);
        assertThat(entity.getRelatedEntityType()).isEqualTo("LESSON");
        assertThat(entity.getRelatedEntityId()).isEqualTo(lessonId);
    }

    @Test
    @DisplayName("uploadFile — підтримує JPEG файли")
    void uploadFile_jpegFile_saves() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "img".getBytes());

        when(minioService.uploadFile(any(), anyString())).thenReturn("general/photo.jpg");
        FileEntity saved = new FileEntity();
        when(fileRepository.save(any())).thenReturn(saved);
        when(fileMapper.toDto(saved)).thenReturn(new FileDto(null, null, null, null, null, null, null, null, null));

        fileStorageService.uploadFile(file, "APPEAL", null, adminUser);

        verify(fileRepository).save(any());
        verify(minioService).uploadFile(any(), eq("appeal"));
    }

    @Test
    @DisplayName("uploadFile — кидає ResourceNotFoundException якщо урок не знайдено для LESSON entity")
    void uploadFile_lessonNotFound_throws() throws Exception {
        UUID lessonId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "data".getBytes());

        when(minioService.uploadFile(any(), anyString())).thenReturn("lesson/doc.pdf");
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileStorageService.uploadFile(file, "LESSON", lessonId, adminUser))
                .isInstanceOf(RuntimeException.class)
                .cause().isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lesson not found");

        verify(fileRepository, never()).save(any());
    }

    // ─────────────────────── deleteFile ───────────────────────

    @Test
    @DisplayName("deleteFile — кидає ResourceNotFoundException якщо файл не знайдено")
    void deleteFile_notFound_throws() throws Exception {
        UUID fileId = UUID.randomUUID();
        when(fileRepository.findById(fileId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fileStorageService.deleteFile(fileId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(minioService, never()).deleteFile(anyString());
        verify(minioService, never()).uploadFile(any(), anyString());
    }

    @Test
    @DisplayName("deleteFile — видаляє файл з MinIO і потім з DB (правильний порядок)")
    void deleteFile_success_deletesFromMinioAndDb() throws Exception {
        UUID fileId = UUID.randomUUID();
        FileEntity entity = new FileEntity();
        entity.setMinioObjectName("lessons/file.pdf");

        when(fileRepository.findById(fileId)).thenReturn(Optional.of(entity));

        fileStorageService.deleteFile(fileId);

        // Порядок має значення: спочатку MinIO, потім DB
        var order = inOrder(minioService, fileRepository);
        order.verify(minioService).deleteFile("lessons/file.pdf");
        order.verify(fileRepository).delete(entity);
    }

    // ─────────────────────── getFilesForEntity ───────────────────────

    @Test
    @DisplayName("getFilesForEntity — повертає mapped DTO список, mapper викликається для кожного")
    void getFilesForEntity_returnsMappedList() {
        FileEntity e1 = new FileEntity();
        e1.setId(UUID.randomUUID());
        FileDto dto1 = new FileDto(UUID.randomUUID(), null, null, null, null, null, null, null, null);

        when(fileRepository.findByRelatedEntityTypeAndRelatedEntityId("APPEAL", adminUser.getId()))
                .thenReturn(List.of(e1));
        when(fileMapper.toDto(e1)).thenReturn(dto1);

        List<FileDto> result = fileStorageService.getFilesForEntity("APPEAL", adminUser.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(dto1);
        verify(fileMapper).toDto(e1);
    }

    @Test
    @DisplayName("getFilesForEntity — порожній список якщо файлів немає")
    void getFilesForEntity_noFiles_returnsEmpty() {
        when(fileRepository.findByRelatedEntityTypeAndRelatedEntityId(anyString(), any()))
                .thenReturn(Collections.emptyList());

        List<FileDto> result = fileStorageService.getFilesForEntity("LESSON", UUID.randomUUID());

        assertThat(result).isEmpty();
        verify(fileMapper, never()).toDto(any());
    }

    // ─────────────────────── getLessonFiles ───────────────────────

    @Test
    @DisplayName("getLessonFiles — ADMIN отримує список файлів уроку")
    void getLessonFiles_admin_returnsFiles() {
        UUID lessonId = UUID.randomUUID();
        FileEntity e = new FileEntity();
        FileDto dto = new FileDto(UUID.randomUUID(), null, null, null, null, null, null, null, null);

        when(fileRepository.findByLessonId(lessonId)).thenReturn(List.of(e));
        when(fileMapper.toDto(e)).thenReturn(dto);

        List<FileDto> result = fileStorageService.getLessonFiles(lessonId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isSameAs(dto);
    }

    @Test
    @DisplayName("getLessonFiles — FAKE_ADMIN також отримує доступ")
    void getLessonFiles_fakeAdmin_returnsFiles() {
        UUID lessonId = UUID.randomUUID();
        PersonEntity fakeAdmin = new PersonEntity();
        fakeAdmin.setEmail("fake@test.com");
        fakeAdmin.setRole(PersonRole.FAKE_ADMIN);

        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));
        when(fileRepository.findByLessonId(lessonId)).thenReturn(List.of());

        List<FileDto> result = fileStorageService.getLessonFiles(lessonId);

        assertThat(result).isNotNull();
        verify(fileRepository).findByLessonId(lessonId);
    }

    @Test
    @DisplayName("getLessonFiles — USER без зарахування отримує порожній список")
    void getLessonFiles_userWithoutEnrollment_returnsEmpty() {
        UUID lessonId = UUID.randomUUID();
        PersonEntity student = new PersonEntity();
        student.setId(UUID.randomUUID());
        student.setEmail("student@test.com");
        student.setRole(PersonRole.USER);

        setSecurityContext("student@test.com", "ROLE_USER");
        when(personRepository.findByEmail("student@test.com")).thenReturn(Optional.of(student));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.empty());

        List<FileDto> result = fileStorageService.getLessonFiles(lessonId);

        assertThat(result).isEmpty();
        verify(fileRepository, never()).findByLessonId(any());
    }
}
