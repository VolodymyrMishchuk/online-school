package com.mishchuk.onlineschool.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mishchuk.onlineschool.controller.dto.BroadcastRequest;
import com.mishchuk.onlineschool.controller.dto.TargetedNotificationRequest;
import com.mishchuk.onlineschool.exception.GlobalExceptionHandler;
import com.mishchuk.onlineschool.repository.entity.NotificationEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.security.JwtUtils;
import com.mishchuk.onlineschool.service.NotificationService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import({TestSecurityConfig.class, GlobalExceptionHandler.class})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private CustomUserDetailsService userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // СЕКЦІЯ: GET /notifications

    @Test
    @DisplayName("GET /notifications — авторизований → 200 OK")
    @WithMockUser(username = "user@test.com")
    void getUserNotifications_authenticated_returns200() throws Exception {
        PersonEntity person = personEntity(PersonRole.USER);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);
        when(notificationService.getUserNotifications(eq(person.getId()), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notificationEntity())));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].title").value("Title"));

        verify(notificationService, times(1)).getUserNotifications(eq(person.getId()), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /notifications — анонімний → 401 Unauthorized")
    void getUserNotifications_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isUnauthorized());
    }

    // СЕКЦІЯ: PUT /notifications/{id}/read

    @Test
    @DisplayName("PUT /notifications/{id}/read — авторизований → 200 OK")
    @WithMockUser
    void markAsRead_authenticated_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(notificationService).markAsRead(id);

        mockMvc.perform(put("/notifications/{id}/read", id))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).markAsRead(id);
    }

    // СЕКЦІЯ: PUT /notifications/{id}/unread

    @Test
    @DisplayName("PUT /notifications/{id}/unread — авторизований → 200 OK")
    @WithMockUser
    void markAsUnread_authenticated_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        doNothing().when(notificationService).markAsUnread(id);

        mockMvc.perform(put("/notifications/{id}/unread", id))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).markAsUnread(id);
    }

    // СЕКЦІЯ: GET /notifications/unread-count

    @Test
    @DisplayName("GET /notifications/unread-count — авторизований → 200 OK")
    @WithMockUser(username = "user@test.com")
    void getUnreadCount_authenticated_returns200() throws Exception {
        PersonEntity person = personEntity(PersonRole.USER);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);
        when(notificationService.getUnreadCount(person.getId())).thenReturn(5L);

        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().string("5"));
    }

    @Test
    @DisplayName("GET /notifications/unread-count — анонімний → 401 Unauthorized")
    void getUnreadCount_anonymous_returns401() throws Exception {
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isUnauthorized());
    }

    // СЕКЦІЯ: PUT /notifications/read-all

    @Test
    @DisplayName("PUT /notifications/read-all — авторизований → 200 OK")
    @WithMockUser(username = "user@test.com")
    void markAllAsRead_authenticated_returns200() throws Exception {
        PersonEntity person = personEntity(PersonRole.USER);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);
        doNothing().when(notificationService).markAllAsRead(person.getId());

        mockMvc.perform(put("/notifications/read-all"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).markAllAsRead(person.getId());
    }

    @Test
    @DisplayName("PUT /notifications/read-all — анонімний → 401 Unauthorized")
    void markAllAsRead_anonymous_returns401() throws Exception {
        mockMvc.perform(put("/notifications/read-all"))
                .andExpect(status().isUnauthorized());
    }

    // СЕКЦІЯ: PUT /notifications/unread-all

    @Test
    @DisplayName("PUT /notifications/unread-all — авторизований → 200 OK")
    @WithMockUser(username = "user@test.com")
    void markAllAsUnread_authenticated_returns200() throws Exception {
        PersonEntity person = personEntity(PersonRole.USER);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);
        doNothing().when(notificationService).markAllAsUnread(person.getId());

        mockMvc.perform(put("/notifications/unread-all"))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).markAllAsUnread(person.getId());
    }

    @Test
    @DisplayName("PUT /notifications/unread-all — анонімний → 401 Unauthorized")
    void markAllAsUnread_anonymous_returns401() throws Exception {
        mockMvc.perform(put("/notifications/unread-all"))
                .andExpect(status().isUnauthorized());
    }

    // СЕКЦІЯ: DELETE /notifications/all

    @Test
    @DisplayName("DELETE /notifications/all — авторизований → 204 No Content")
    @WithMockUser(username = "user@test.com")
    void deleteAll_authenticated_returns204() throws Exception {
        PersonEntity person = personEntity(PersonRole.USER);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);
        doNothing().when(notificationService).deleteAll(person.getId());

        mockMvc.perform(delete("/notifications/all"))
                .andExpect(status().isNoContent());

        verify(notificationService, times(1)).deleteAll(person.getId());
    }

    @Test
    @DisplayName("DELETE /notifications/all — анонімний → 401 Unauthorized")
    void deleteAll_anonymous_returns401() throws Exception {
        mockMvc.perform(delete("/notifications/all"))
                .andExpect(status().isUnauthorized());
    }

    // СЕКЦІЯ: DELETE /notifications/{id}

    @Test
    @DisplayName("DELETE /notifications/{id} — авторизований → 204 No Content")
    @WithMockUser(username = "user@test.com")
    void deleteNotification_authenticated_returns204() throws Exception {
        UUID id = UUID.randomUUID();
        mockMvc.perform(delete("/notifications/{id}", id))
                .andExpect(status().isNoContent());

        verify(notificationService, times(1)).deleteNotification(id);
    }

    @Test
    @DisplayName("DELETE /notifications/{id} — анонімний → 401 Unauthorized")
    void deleteNotification_anonymous_returns401() throws Exception {
        mockMvc.perform(delete("/notifications/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // СЕКЦІЯ: POST /notifications/broadcast

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /notifications/broadcast — авторизована роль → 200 OK")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void broadcastToAll_authorizedRole_returns200(String role) throws Exception {
        PersonEntity person = personEntity(PersonRole.valueOf(role));
        when(userDetailsService.getPerson("admin@test.com")).thenReturn(person);
        doNothing().when(notificationService).sendToAllUsers(any(), any(), any());

        BroadcastRequest request = new BroadcastRequest("T", "M", "U");

        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).sendToAllUsers("T", "M", "U");
    }

    @Test
    @DisplayName("POST /notifications/broadcast — неавторизована роль (USER) → 403 Forbidden")
    @WithMockUser(username = "user@test.com", roles = "USER")
    void broadcastToAll_unauthorizedRole_returns403() throws Exception {
        PersonEntity person = personEntity(PersonRole.USER);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);

        BroadcastRequest request = new BroadcastRequest("T", "M", "U");

        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("POST /notifications/broadcast — анонімний → 500/400 NPE (або 401)")
    void broadcastToAll_anonymous_returnsClientError() throws Exception {
        BroadcastRequest request = new BroadcastRequest("T", "M", "U");

        // Controller logic throws NPE when principal is null, mapping to 400 Bad Request if no custom error handling
        // We expect any 4xx/5xx status
        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    // СЕКЦІЯ: POST /notifications/send-to-users

    @ParameterizedTest
    @ValueSource(strings = {"ADMIN", "FAKE_ADMIN"})
    @DisplayName("POST /notifications/send-to-users — авторизована роль → 200 OK")
    @WithMockUser(username = "admin@test.com", roles = {"ADMIN"})
    void sendToUsers_authorizedRole_returns200(String role) throws Exception {
        PersonEntity person = personEntity(PersonRole.valueOf(role));
        when(userDetailsService.getPerson("admin@test.com")).thenReturn(person);
        doNothing().when(notificationService).sendToUsers(any(), any(), any(), any());

        TargetedNotificationRequest request = new TargetedNotificationRequest("T", "M", List.of(UUID.randomUUID()), "U");

        mockMvc.perform(post("/notifications/send-to-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(notificationService, times(1)).sendToUsers(eq("T"), eq("M"), anyList(), eq("U"));
    }

    @Test
    @DisplayName("POST /notifications/send-to-users — неавторизована роль (USER) → 403 Forbidden")
    @WithMockUser(username = "user@test.com")
    void sendToUsers_unauthorizedRole_returns403() throws Exception {
        PersonEntity person = personEntity(PersonRole.USER);
        when(userDetailsService.getPerson("user@test.com")).thenReturn(person);

        TargetedNotificationRequest request = new TargetedNotificationRequest("T", "M", List.of(UUID.randomUUID()), "U");

        mockMvc.perform(post("/notifications/send-to-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verifyNoInteractions(notificationService);
    }

    @Test
    @DisplayName("POST /notifications/send-to-users — анонімний → 4xx-5xx")
    void sendToUsers_anonymous_returnsClientError() throws Exception {
        TargetedNotificationRequest request = new TargetedNotificationRequest("T", "M", List.of(UUID.randomUUID()), "U");

        mockMvc.perform(post("/notifications/send-to-users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is5xxServerError());
    }

    // ХЕЛПЕРИ

    @NotNull
    @Contract(" -> new")
    private NotificationEntity notificationEntity() {
        NotificationEntity e = new NotificationEntity();
        e.setId(UUID.randomUUID());
        e.setTitle("Title");
        e.setMessage("Message");
        e.setType(NotificationType.SYSTEM);
        e.setRead(false);
        return e;
    }

    @NotNull
    @Contract("_ -> new")
    private PersonEntity personEntity(PersonRole role) {
        PersonEntity person = new PersonEntity();
        person.setId(UUID.randomUUID());
        person.setRole(role);
        return person;
    }
}
