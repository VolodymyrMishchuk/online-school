package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.FileStorageService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private PersonRepository personRepository;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: POST /files/upload

    @Test
    @DisplayName("POST /files/upload — авторизований → 200 OK")
    @WithMockUser(username = "user@test.com")
    void uploadFile_authenticated_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());
        PersonEntity person = new PersonEntity();
        person.setId(UUID.randomUUID());

        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(person));
        when(fileStorageService.uploadFile(any(), eq("COURSE"), any(), eq(person))).thenReturn(fileDto());

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("entityType", "COURSE")
                        .param("entityId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("test.jpg"));
    }

    @Test
    @DisplayName("POST /files/upload — анонімний (дозволено контролером) → 200 OK")
    void uploadFile_anonymous_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes());

        when(fileStorageService.uploadFile(any(), eq("COURSE"), any(), isNull())).thenReturn(fileDto());

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .param("entityType", "COURSE")
                        .param("entityId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalName").value("test.jpg"));
    }

    // СЕКЦІЯ: GET /files/{fileId}

    @Test
    @DisplayName("GET /files/{fileId} — завантаження файлу → 200 OK")
    void downloadFile_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        com.mishchuk.onlineschool.repository.entity.FileEntity metadata = new com.mishchuk.onlineschool.repository.entity.FileEntity();
        metadata.setOriginalName("test.jpg");
        metadata.setContentType("image/jpeg");
        metadata.setFileSize(7L);
        FileStorageService.FileDownloadDto downloadDto = new FileStorageService.FileDownloadDto(
                new ByteArrayInputStream("content".getBytes()),
                metadata
        );

        when(fileStorageService.downloadFile(id)).thenReturn(downloadDto);

        mockMvc.perform(get("/files/{fileId}", id))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.jpg\""))
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes("content".getBytes()));
    }

    // СЕКЦІЯ: DELETE /files/{fileId}

    @Test
    @DisplayName("DELETE /files/{fileId} — видалення файлу → 204 No Content")
    void deleteFile_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(fileStorageService).deleteFile(id);

        mockMvc.perform(delete("/files/{fileId}", id))
                .andExpect(status().isNoContent());

        verify(fileStorageService, times(1)).deleteFile(id);
    }

    // СЕКЦІЯ: GET /files/entity/{entityType}/{entityId}

    @Test
    @DisplayName("GET /files/entity/{entityType}/{entityId} — список файлів → 200 OK")
    void getFilesForEntity_returns200() throws Exception {
        UUID entityId = UUID.randomUUID();
        when(fileStorageService.getFilesForEntity("COURSE", entityId)).thenReturn(List.of(fileDto()));

        mockMvc.perform(get("/files/entity/{entityType}/{entityId}", "COURSE", entityId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].originalName").value("test.jpg"));
    }

    // СЕКЦІЯ: GET /files/my-files

    @Test
    @DisplayName("GET /files/my-files — авторизований → 200 OK")
    @WithMockUser(username = "user@test.com")
    void getMyFiles_authenticated_returns200() throws Exception {
        PersonEntity person = new PersonEntity();
        person.setId(UUID.randomUUID());

        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(person));
        when(fileStorageService.getFilesByUser(person)).thenReturn(List.of(fileDto()));

        mockMvc.perform(get("/files/my-files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].originalName").value("test.jpg"));
    }

    @Test
    @DisplayName("GET /files/my-files — анонімний → 401 Unauthorized")
    void getMyFiles_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/files/my-files"))
                .andExpect(status().isUnauthorized());
    }

    // ХЕЛПЕРИ

    @NotNull
    private FileDto fileDto() {
        return FileDto.builder()
                .id(UUID.randomUUID())
                .fileName("test.jpg")
                .originalName("test.jpg")
                .contentType("image/jpeg")
                .fileSize(1024L)
                .uploadedAt(java.time.LocalDateTime.now())
                .downloadUrl("/files/123")
                .relatedEntityType("COURSE")
                .relatedEntityId(UUID.randomUUID())
                .build();
    }
}
