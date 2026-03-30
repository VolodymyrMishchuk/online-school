package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.controller.dto.CreatedByDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.ModuleService;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ModuleController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class ModuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ModuleService moduleService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: POST /modules

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /modules — авторизована роль → 201 Created")
    void createModule_authorizedRole_returns201(String role) throws Exception {
        doNothing().when(moduleService).createModule(any());

        mockMvc.perform(post("/modules")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isCreated());

        verify(moduleService, times(1)).createModule(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /modules — неавторизована роль → 403 Forbidden")
    void createModule_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(post("/modules")
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    @DisplayName("POST /modules — анонімний → 403 Forbidden")
    void createModule_anonymous_returns403() throws Exception {
        mockMvc.perform(post("/modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto())))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: GET /modules/{id}

    @Test
    @DisplayName("GET /modules/{id} — знайдено → 200 OK")
    @WithMockUser
    void getModule_found_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(moduleService.getModule(eq(id))).thenReturn(Optional.of(moduleDto(id, "Module 1")));

        mockMvc.perform(get("/modules/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Module 1"));
    }

    @Test
    @DisplayName("GET /modules/{id} — не знайдено → 404 Not Found")
    @WithMockUser
    void getModule_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(moduleService.getModule(eq(id))).thenReturn(Optional.empty());

        mockMvc.perform(get("/modules/{id}", id))
                .andExpect(status().isNotFound());
    }

    // СЕКЦІЯ: GET /modules

    @Test
    @DisplayName("GET /modules — авторизований (без courseId) → 200 OK")
    @WithMockUser
    void getAllModules_withoutCourseId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(moduleService.getAllModules(null)).thenReturn(List.of(moduleDto(id, "Module All")));

        mockMvc.perform(get("/modules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(id.toString()))
                .andExpect(jsonPath("$[0].name").value("Module All"));

        verify(moduleService, times(1)).getAllModules(null);
    }

    @Test
    @DisplayName("GET /modules?courseId={id} — авторизований → 200 OK")
    @WithMockUser
    void getAllModules_withCourseId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        UUID courseId = UUID.randomUUID();
        when(moduleService.getAllModules(eq(courseId))).thenReturn(List.of(moduleDto(id, "Module Course")));

        mockMvc.perform(get("/modules").param("courseId", courseId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Module Course"));

        verify(moduleService, times(1)).getAllModules(eq(courseId));
    }

    // СЕКЦІЯ: GET /modules/{id}/lessons

    @Test
    @DisplayName("GET /modules/{id}/lessons — авторизований → 200 OK")
    @WithMockUser
    void getModuleLessons_authorized_returns200() throws Exception {
        UUID moduleId = UUID.randomUUID();
        UUID lessonId = UUID.randomUUID();
        when(moduleService.getModuleLessons(eq(moduleId))).thenReturn(List.of(lessonDto(lessonId, "Lesson 1")));

        mockMvc.perform(get("/modules/{id}/lessons", moduleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(lessonId.toString()))
                .andExpect(jsonPath("$[0].name").value("Lesson 1"));

        verify(moduleService, times(1)).getModuleLessons(eq(moduleId));
    }

    // СЕКЦІЯ: PUT /modules/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("PUT /modules/{id} — авторизована роль → 204 No Content")
    void updateModule_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(moduleService).updateModule(eq(id), any());

        mockMvc.perform(put("/modules/{id}", id)
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isNoContent());

        verify(moduleService, times(1)).updateModule(eq(id), any());
    }

    @Test
    @DisplayName("PUT /modules/{id} — сервіс кидає виняток → 404 Not Found")
    @WithMockUser(roles = "ADMIN")
    void updateModule_serviceThrows_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new RuntimeException("Not found")).when(moduleService).updateModule(any(), any());

        mockMvc.perform(put("/modules/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("PUT /modules/{id} — неавторизована роль → 403 Forbidden")
    void updateModule_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(put("/modules/{id}", UUID.randomUUID())
                        .with(user("u").roles(role))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    @DisplayName("PUT /modules/{id} — анонімний → 403 Forbidden")
    void updateModule_anonymous_returns403() throws Exception {
        mockMvc.perform(put("/modules/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto())))
                .andExpect(status().isForbidden());
    }

    // СЕКЦІЯ: DELETE /modules/{id}

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("DELETE /modules/{id} — авторизована роль → 204 No Content")
    void deleteModule_authorizedRole_returns204(String role) throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(moduleService).deleteModule(id);

        mockMvc.perform(delete("/modules/{id}", id).with(user("u").roles(role)))
                .andExpect(status().isNoContent());

        verify(moduleService, times(1)).deleteModule(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("DELETE /modules/{id} — неавторизована роль → 403 Forbidden")
    void deleteModule_unauthorizedRole_returns403(String role) throws Exception {
        mockMvc.perform(delete("/modules/{id}", UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(moduleService);
    }

    @Test
    @DisplayName("DELETE /modules/{id} — анонімний → 403 Forbidden")
    void deleteModule_anonymous_returns403() throws Exception {
        mockMvc.perform(delete("/modules/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    // ХЕЛПЕРИ (FACTORY METHODS)

    @NotNull
    @Contract("_, _ -> new")
    private ModuleDto moduleDto(UUID id, String name) {
        return new ModuleDto(
                id, name, UUID.randomUUID(), "Desc", 5, 120, "PUBLISHED", null, null, new CreatedByDto(UUID.randomUUID(), "Admin", "Adminovych", "admin@test.com")
        );
    }

    @NotNull
    @Contract("_, _ -> new")
    private LessonDto lessonDto(UUID id, String name) {
        return new LessonDto(id, UUID.randomUUID(), name, "desc", null, null, null, null, null, null, null, null);
    }

    @NotNull
    @Contract(" -> new")
    private ModuleCreateDto createDto() {
        return new ModuleCreateDto("Module 1", UUID.randomUUID(), "Desc", List.of(UUID.randomUUID()));
    }

    @NotNull
    @Contract(" -> new")
    private ModuleUpdateDto updateDto() {
        return new ModuleUpdateDto("Module Updated", "Desc", "DRAFT", List.of(UUID.randomUUID()));
    }
}
