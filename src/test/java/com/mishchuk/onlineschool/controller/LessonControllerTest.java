package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.controller.dto.*;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.service.FileStorageService;
import com.mishchuk.onlineschool.service.LessonService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LessonController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class LessonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LessonService lessonService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private FileStorageService fileStorageService;

    // СЕКЦІЯ: POST /lessons

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /lessons — авторизована роль → 201 Created")
    void createLesson_authorizedRole_returns201(String role) throws Exception {
        UUID id = UUID.randomUUID();
        when(lessonService.createLesson(any())).thenReturn(lessonDto(id, "Created"));

        mockMvc.perform(post("/lessons")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Created"));

        verify(lessonService, times(1)).createLesson(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /lessons — неавторизована роль → 403 Forbidden")
    void createLesson_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/lessons")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService, fileStorageService);
    }

    @Test
    @DisplayName("POST /lessons — анонімний → 403 Forbidden")
    void createLesson_anonymous_returns403() throws Exception {
        mockMvc.perform(post("/lessons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /lessons

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("GET /lessons — авторизована роль → 200 OK")
    void getAllLessons_authorizedRole_returns200(String role) throws Exception {
        UUID id = UUID.randomUUID();
        when(lessonService.getAllLessons()).thenReturn(List.of(lessonDto(id, "Test")));

        mockMvc.perform(get("/lessons").with(user("u").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Test"));

        verify(lessonService, times(1)).getAllLessons();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("GET /lessons — неавторизована роль → 403 Forbidden")
    void getAllLessons_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/lessons").with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService, fileStorageService);
    }

    @Test
    @DisplayName("GET /lessons — анонімний → 403 Forbidden")
    void getAllLessons_anonymous_returns403() throws Exception {
        mockMvc.perform(get("/lessons"))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /lessons/unassigned

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("GET /lessons/unassigned — авторизована роль → 200 OK")
    void getUnassignedLessons_authorizedRole_returns200(String role) throws Exception {
        UUID id = UUID.randomUUID();
        when(lessonService.getUnassignedLessons()).thenReturn(List.of(lessonDto(id, "Unassigned")));

        mockMvc.perform(get("/lessons/unassigned").with(user("u").roles(role)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Unassigned"));

        verify(lessonService, times(1)).getUnassignedLessons();
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("GET /lessons/unassigned — неавторизована роль → 403 Forbidden")
    void getUnassignedLessons_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/lessons/unassigned").with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService, fileStorageService);
    }

    @Test
    @DisplayName("GET /lessons/unassigned — анонімний → 403 Forbidden")
    void getUnassignedLessons_anonymous_returns403() throws Exception {
        mockMvc.perform(get("/lessons/unassigned"))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /lessons/{id}

    @Test
    @DisplayName("GET /lessons/{id} — знайдено → 200 OK")
    @WithMockUser
    void getLesson_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(lessonService.getLesson(eq(id))).thenReturn(Optional.of(lessonDto(id, "Found")));

        mockMvc.perform(get("/lessons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Found"));
    }

    @Test
    @DisplayName("GET /lessons/{id} — не знайдено → 404 Not Found")
    @WithMockUser
    void getLesson_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(lessonService.getLesson(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/lessons/{id}", id))
                .andExpect(status().isNotFound());
    }

    // СЕКЦІЯ: GET /lessons/{id}/files

    @Test
    @DisplayName("GET /lessons/{id}/files — знайдено → 200 OK")
    @WithMockUser
    void getLessonFiles_returns200() throws Exception {
        UUID lessonId = UUID.randomUUID();
        when(fileStorageService.getLessonFiles(lessonId)).thenReturn(List.of(fileDto()));

        mockMvc.perform(get("/lessons/{id}/files", lessonId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("file.pdf"));
    }

    // СЕКЦІЯ: PUT /lessons/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PUT /lessons/{id} — авторизована роль → 204 No Content")
    void updateLesson_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(lessonService).updateLesson(eq(id), any());

        mockMvc.perform(put("/lessons/{id}", id)
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isNoContent());

        verify(lessonService, times(1)).updateLesson(eq(id), any());
    }

    @Test
    @DisplayName("PUT /lessons/{id} — сервіс кидає виняток → 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void updateLesson_serviceThrows_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Not found")).when(lessonService).updateLesson(any(), any());

        mockMvc.perform(put("/lessons/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("PUT /lessons/{id} — неавторизована роль → 403 Forbidden")
    void updateLesson_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(put("/lessons/{id}", UUID.randomUUID())
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService, fileStorageService);
    }

    @Test
    @DisplayName("PUT /lessons/{id} — анонімний → 403 Forbidden")
    void updateLesson_anonymous_returns403() throws Exception {
        mockMvc.perform(put("/lessons/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: DELETE /lessons/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("DELETE /lessons/{id} — авторизована роль → 204 No Content")
    void deleteLesson_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(lessonService).deleteLesson(id);

        mockMvc.perform(delete("/lessons/{id}", id).with(user("u").roles(role)))
                .andExpect(status().isNoContent());

        verify(lessonService, times(1)).deleteLesson(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("DELETE /lessons/{id} — неавторизована роль → 403 Forbidden")
    void deleteLesson_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(delete("/lessons/{id}", UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(lessonService, fileStorageService);
    }

    @Test
    @DisplayName("DELETE /lessons/{id} — анонімний → 403 Forbidden")
    void deleteLesson_anonymous_returns403() throws Exception {
        mockMvc.perform(delete("/lessons/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // ХЕЛПЕРИ (FACTORY METHODS)

    @NotNull
    @Contract(" -> new")
    private FileDto fileDto() {
        return new FileDto(UUID.randomUUID(), "file.pdf", "http://storage.com/file.pdf", null, null, null, null, null, null);
    }

    @NotNull
    @Contract("_, _ -> new")
    private LessonDto lessonDto(UUID id, String name) {
        return new LessonDto(id, UUID.randomUUID(), name, "desc", null, null, null, null, null, null, null, null);
    }

    @NotNull
    @Contract(" -> new")
    private LessonCreateDto createDto() {
        return new LessonCreateDto(UUID.randomUUID(), "New Lesson", "Description", null, null);
    }

    @NotNull
    @Contract(" -> new")
    private LessonUpdateDto updateDto() {
        return new LessonUpdateDto("Updated Title", "Updated Description", null, null);
    }
}