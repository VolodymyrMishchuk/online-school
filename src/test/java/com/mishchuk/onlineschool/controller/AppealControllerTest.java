package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.AppealResponse;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.repository.entity.AppealStatus;
import com.mishchuk.onlineschool.repository.entity.ContactMethod;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.AppealService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AppealController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class AppealControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppealService appealService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: POST /appeals (authenticated)

    @Test
    @DisplayName("POST /appeals — авторизований + валідний запит → 201 Created")
    @WithMockUser(username = "user@test.com")
    void createAppeal_authenticatedValidRequest_returns201() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        PersonEntity person = personEntity(userId);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);
        when(appealService.createAppeal(eq(userId), any(), any())).thenReturn(appealResponse());

        mockMvc.perform(multipart("/appeals")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "user@test.com")
                        .param("message", "Текст звернення")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000010"))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(appealService, times(1)).createAppeal(eq(userId), any(), any());
    }

    @Test
    @DisplayName("POST /appeals — авторизований + фото → 201 Created")
    @WithMockUser(username = "user@test.com")
    void createAppeal_authenticatedWithPhotos_returns201() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        PersonEntity person = personEntity(userId);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);
        when(appealService.createAppeal(eq(userId), any(), any())).thenReturn(appealResponse());

        MockMultipartFile photo = new MockMultipartFile(
                "photos", "photo.jpg", "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/appeals")
                        .file(photo)
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "user@test.com")
                        .param("message", "Текст звернення")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());

        verify(appealService, times(1)).createAppeal(eq(userId), any(), any());
    }

    @Test
    @DisplayName("POST /appeals — невалідний запит (порожнє message) → 400 Bad Request")
    @WithMockUser(username = "user@test.com")
    void createAppeal_blankMessage_returns400() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(userDetailsService.getPerson("user@test.com"))
                .thenReturn(personEntity(userId));

        mockMvc.perform(multipart("/appeals")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "user@test.com")
                        .param("message", "")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("POST /appeals — невалідний запит (відсутній contactMethod) → 400 Bad Request")
    @WithMockUser(username = "user@test.com")
    void createAppeal_nullContactMethod_returns400() throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(userDetailsService.getPerson("user@test.com"))
                .thenReturn(personEntity(userId));

        mockMvc.perform(multipart("/appeals")
                        .param("contactDetails", "user@test.com")
                        .param("message", "Текст повідомлення")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("POST /appeals — анонімний → 401 Unauthorized")
    void createAppeal_anonymous_returns401() throws Exception {
        mockMvc.perform(multipart("/appeals")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "user@test.com")
                        .param("message", "Текст звернення")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(appealService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER"})
    @DisplayName("POST /appeals — будь-яка авторизована роль → 201 (ендпоінт не обмежений роллю)")
    void createAppeal_anyAuthenticatedRole_returns201(String role) throws Exception {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        when(userDetailsService.getPerson("user@test.com"))
                .thenReturn(personEntity(userId));
        when(appealService.createAppeal(eq(userId), any(), any())).thenReturn(appealResponse());

        mockMvc.perform(multipart("/appeals")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "user@test.com")
                        .param("message", "Текст звернення")
                        .with(user("user@test.com").roles(role))
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    // СЕКЦІЯ: POST /appeals/public

    @Test
    @DisplayName("POST /appeals/public — валідний запит → 201 Created")
    void createPublicAppeal_validRequest_returns201() throws Exception {
        when(appealService.createPublicAppeal(any(), any())).thenReturn(appealResponse());

        mockMvc.perform(multipart("/appeals/public")
                        .param("name", "Гість")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "guest@test.com")
                        .param("message", "Текст публічного звернення")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000010"))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(appealService, times(1)).createPublicAppeal(any(), any());
    }

    @Test
    @DisplayName("POST /appeals/public — з фото → 201 Created")
    void createPublicAppeal_withPhotos_returns201() throws Exception {
        when(appealService.createPublicAppeal(any(), any())).thenReturn(appealResponse());

        MockMultipartFile photo = new MockMultipartFile(
                "photos", "photo.jpg", "image/jpeg", "fake-image-content".getBytes());

        mockMvc.perform(multipart("/appeals/public")
                        .file(photo)
                        .param("name", "Гість")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "guest@test.com")
                        .param("message", "Текст публічного звернення")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /appeals/public — невалідний запит (порожнє name) → 400 Bad Request")
    void createPublicAppeal_blankName_returns400() throws Exception {
        mockMvc.perform(multipart("/appeals/public")
                        .param("name", "")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "guest@test.com")
                        .param("message", "Текст")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("POST /appeals/public — невалідний запит (порожнє message) → 400 Bad Request")
    void createPublicAppeal_blankMessage_returns400() throws Exception {
        mockMvc.perform(multipart("/appeals/public")
                        .param("name", "Guest")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "guest@test.com")
                        .param("message", "")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("POST /appeals/public — анонімний дозволено (permitAll) → 201 Created")
    void createPublicAppeal_anonymous_isPermitted() throws Exception {
        when(appealService.createPublicAppeal(any(), any())).thenReturn(appealResponse());

        mockMvc.perform(multipart("/appeals/public")
                        .param("name", "Гість")
                        .param("contactMethod", "EMAIL")
                        .param("contactDetails", "guest@test.com")
                        .param("message", "Текст публічного звернення")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated());
    }

    // СЕКЦІЯ: GET /appeals

    @Test
    @DisplayName("GET /appeals — ADMIN → 200 OK + пагінація")
    @WithMockUser(roles = "ADMIN")
    void getAppeals_admin_returns200WithPage() throws Exception {
        when(appealService.getAppeals(any())).thenReturn(
                new PageImpl<>(List.of(appealResponse()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/appeals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("00000000-0000-0000-0000-000000000010"))
                .andExpect(jsonPath("$.content[0].status").value("NEW"))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(appealService, times(1)).getAppeals(any());
    }

    @Test
    @DisplayName("GET /appeals — ADMIN + кастомна пагінація → 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getAppeals_admin_customPagination_returns200() throws Exception {
        when(appealService.getAppeals(eq(PageRequest.of(2, 5))))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        mockMvc.perform(get("/appeals").param("page", "2").param("size", "5"))
                .andExpect(status().isOk());

        verify(appealService, times(1)).getAppeals(eq(PageRequest.of(2, 5)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER", "FAKE_ADMIN"})
    @DisplayName("GET /appeals — не ADMIN роль → 403 Forbidden")
    void getAppeals_nonAdminRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/appeals").with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("GET /appeals — анонімний → 401 Unauthorized")
    void getAppeals_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/appeals"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    // СЕКЦІЯ: GET /appeals/{id}

    @Test
    @DisplayName("GET /appeals/{id} — ADMIN + існуючий id → 200 OK")
    @WithMockUser(roles = "ADMIN")
    void getAppeal_admin_returns200() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        when(appealService.getAppeal(id)).thenReturn(appealResponse());

        mockMvc.perform(get("/appeals/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("NEW"));

        verify(appealService, times(1)).getAppeal(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER", "FAKE_ADMIN"})
    @DisplayName("GET /appeals/{id} — не ADMIN роль → 403 Forbidden")
    void getAppeal_nonAdminRole_returns403(String role) throws Exception {
        mockMvc.perform(get("/appeals/{id}", UUID.randomUUID()).with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("GET /appeals/{id} — анонімний → 401 Unauthorized")
    void getAppeal_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/appeals/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    // СЕКЦІЯ: PATCH /appeals/{id}/status

    @Test
    @DisplayName("PATCH /appeals/{id}/status — ADMIN + NEW → 200 OK")
    @WithMockUser(roles = "ADMIN")
    void updateAppealStatus_adminNewStatus_returns200() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        AppealResponse updated = appealResponse();
        updated.setStatus(AppealStatus.PROCESSED);
        when(appealService.updateAppealStatus(id, AppealStatus.PROCESSED)).thenReturn(updated);

        mockMvc.perform(patch("/appeals/{id}/status", id)
                        .param("status", "PROCESSED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSED"));

        verify(appealService, times(1)).updateAppealStatus(id, AppealStatus.PROCESSED);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER", "FAKE_ADMIN"})
    @DisplayName("PATCH /appeals/{id}/status — не ADMIN роль → 403 Forbidden")
    void updateAppealStatus_nonAdminRole_returns403(String role) throws Exception {
        mockMvc.perform(patch("/appeals/{id}/status", UUID.randomUUID())
                        .param("status", "PROCESSED")
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("PATCH /appeals/{id}/status — анонімний → 401 Unauthorized")
    void updateAppealStatus_anonymous_returns401() throws Exception {
        mockMvc.perform(patch("/appeals/{id}/status", UUID.randomUUID())
                        .param("status", "PROCESSED"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    // СЕКЦІЯ: DELETE /appeals/{id}

    @Test
    @DisplayName("DELETE /appeals/{id} — ADMIN → 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void deleteAppeal_admin_returns204() throws Exception {
        UUID id = UUID.fromString("00000000-0000-0000-0000-000000000010");
        doNothing().when(appealService).deleteAppeal(id);

        mockMvc.perform(delete("/appeals/{id}", id))
                .andExpect(status().isNoContent());

        verify(appealService, times(1)).deleteAppeal(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"USER", "FAKE_USER", "FAKE_ADMIN"})
    @DisplayName("DELETE /appeals/{id} — не ADMIN роль → 403 Forbidden")
    void deleteAppeal_nonAdminRole_returns403(String role) throws Exception {
        mockMvc.perform(delete("/appeals/{id}", UUID.randomUUID())
                        .with(user("u").roles(role)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    @Test
    @DisplayName("DELETE /appeals/{id} — анонімний → 401 Unauthorized")
    void deleteAppeal_anonymous_returns401() throws Exception {
        mockMvc.perform(delete("/appeals/{id}", UUID.randomUUID()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(appealService);
    }

    // ХЕЛПЕРИ (FACTORY METHODS)

    @NotNull
    @Contract(" -> new")
    private AppealResponse appealResponse() {
        AppealResponse response = new AppealResponse();
        response.setId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        response.setStatus(AppealStatus.NEW);
        response.setContactMethod(ContactMethod.EMAIL);
        response.setContactDetails("user@test.com");
        response.setMessage("Текст звернення");
        response.setCreatedAt(OffsetDateTime.parse("2025-01-01T12:00:00+00:00"));
        return response;
    }

    @NotNull
    @Contract("_ -> new")
    private PersonEntity personEntity(UUID id) {
        PersonEntity person = new PersonEntity();
        person.setId(id);
        return person;
    }
}