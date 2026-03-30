package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.repository.NotificationRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private PersonRepository personRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID recipientId;
    private PersonEntity recipient;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
        recipient = new PersonEntity();
        recipient.setId(recipientId);
        recipient.setRole(PersonRole.USER);
    }

    private void mockSecurityContext(String email) {
        var auth = new UsernamePasswordAuthenticationToken(email, null);
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // --- createNotification ---

    @Test
    @DisplayName("createNotification — створює та зберігає сповіщення")
    void createNotification_success() {
        when(personRepository.findById(recipientId)).thenReturn(Optional.of(recipient));
        NotificationEntity saved = new NotificationEntity();
        when(notificationRepository.save(any(NotificationEntity.class))).thenReturn(saved);

        NotificationEntity result = notificationService.createNotification(
                recipientId, "Заголовок", "Текст", NotificationType.GENERIC, null
        );

        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(NotificationEntity.class));
    }

    @Test
    @DisplayName("createNotification — кидає ResourceNotFoundException якщо одержувач не знайдений")
    void createNotification_recipientNotFound_throws() {
        when(personRepository.findById(recipientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                notificationService.createNotification(recipientId, "t", "m", NotificationType.GENERIC, null)
        ).isInstanceOf(ResourceNotFoundException.class);
    }

    // --- broadcastToAdmins ---

    @Test
    @DisplayName("broadcastToAdmins — надсилає сповіщення всім ADMIN-ам")
    void broadcastToAdmins_sendsToAllAdmins() {
        mockSecurityContext("admin@test.com");
        PersonEntity admin = new PersonEntity();
        admin.setRole(PersonRole.ADMIN);
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        PersonEntity admin2 = new PersonEntity();
        when(personRepository.findAllByRole(PersonRole.ADMIN)).thenReturn(List.of(admin2));

        notificationService.broadcastToAdmins("t", "m", NotificationType.SYSTEM, null);

        verify(notificationRepository).saveAll(anyList());
    }

    // --- markAsRead ---

    @Test
    @DisplayName("markAsRead — позначає сповіщення як прочитане")
    void markAsRead_success() {
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity();
        notif.setRead(false);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));

        notificationService.markAsRead(notifId);

        assertThat(notif.isRead()).isTrue();
        verify(notificationRepository).save(notif);
    }

    @Test
    @DisplayName("markAsRead — кидає ResourceNotFoundException якщо не знайдено")
    void markAsRead_notFound_throws() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.findById(notifId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(notifId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- markAsUnread ---

    @Test
    @DisplayName("markAsUnread — позначає сповіщення як непрочитане")
    void markAsUnread_success() {
        UUID notifId = UUID.randomUUID();
        NotificationEntity notif = new NotificationEntity();
        notif.setRead(true);

        when(notificationRepository.findById(notifId)).thenReturn(Optional.of(notif));

        notificationService.markAsUnread(notifId);

        assertThat(notif.isRead()).isFalse();
        verify(notificationRepository).save(notif);
    }

    // --- deleteNotification ---

    @Test
    @DisplayName("deleteNotification — видаляє якщо існує")
    void deleteNotification_success() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.existsById(notifId)).thenReturn(true);

        notificationService.deleteNotification(notifId);

        verify(notificationRepository).deleteById(notifId);
    }

    @Test
    @DisplayName("deleteNotification — кидає ResourceNotFoundException якщо не існує")
    void deleteNotification_notFound_throws() {
        UUID notifId = UUID.randomUUID();
        when(notificationRepository.existsById(notifId)).thenReturn(false);

        assertThatThrownBy(() -> notificationService.deleteNotification(notifId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // --- getUnreadCount ---

    @Test
    @DisplayName("getUnreadCount — повертає кількість непрочитаних")
    void getUnreadCount_returnsValue() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(recipientId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(recipientId);

        assertThat(count).isEqualTo(5L);
    }
}
