package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.repository.NotificationRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.NotificationEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.ArrayList;
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
    private PersonEntity adminUser;
    private PersonEntity fakeAdmin;

    @BeforeEach
    void setUp() {
        recipientId = UUID.randomUUID();
        
        recipient = new PersonEntity();
        recipient.setId(recipientId);
        recipient.setRole(PersonRole.USER);

        adminUser = new PersonEntity();
        adminUser.setId(UUID.randomUUID());
        adminUser.setEmail("admin@test.com");
        adminUser.setRole(PersonRole.ADMIN);

        fakeAdmin = new PersonEntity();
        fakeAdmin.setId(UUID.randomUUID());
        fakeAdmin.setEmail("fake@test.com");
        fakeAdmin.setRole(PersonRole.FAKE_ADMIN);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void setSecurityContext(String email, String role) {
        var auth = new UsernamePasswordAuthenticationToken(email, null, List.of(new SimpleGrantedAuthority(role)));
        SecurityContextHolder.setContext(new SecurityContextImpl(auth));
    }

    // ─────────────────────── createNotification ───────────────────────

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

        verify(notificationRepository, never()).save(any());
    }

    // ─────────────────────── broadcastToAdmins ───────────────────────

    @Test
    @DisplayName("broadcastToAdmins (ADMIN) — надсилає сповіщення всім ADMIN-ам")
    void broadcastToAdmins_admin_sendsToAllAdmins() {
        setSecurityContext("admin@test.com", "ROLE_ADMIN");
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        
        PersonEntity admin2 = new PersonEntity();
        when(personRepository.findAllByRole(PersonRole.ADMIN)).thenReturn(List.of(admin2));

        notificationService.broadcastToAdmins("title", "msg", NotificationType.SYSTEM, null);

        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("broadcastToAdmins (FAKE_ADMIN) — надсилає сповіщення лише собі")
    void broadcastToAdmins_fakeAdmin_sendsOnlyToSelf() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        notificationService.broadcastToAdmins("title", "msg", NotificationType.SYSTEM, null);

        // verify findAllByRole was never called, because fake admin scopes to self
        verify(personRepository, never()).findAllByRole(PersonRole.ADMIN);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NotificationEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<NotificationEntity> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getRecipient()).isEqualTo(fakeAdmin);
    }

    // ─────────────────────── sendToAllUsers ───────────────────────

    @Test
    @DisplayName("sendToAllUsers (ADMIN) — надсилає всім користувачам платформи")
    void sendToAllUsers_admin_sendsToAll() {
        setSecurityContext("admin@test.com", "ROLE_ADMIN");
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(adminUser));
        when(personRepository.findAll()).thenReturn(List.of(recipient));

        notificationService.sendToAllUsers("title", "msg", null);

        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("sendToAllUsers (FAKE_ADMIN) — надсилає тільки собі та своїм створеним користувачам")
    void sendToAllUsers_fakeAdmin_scopedToSelfAndCreated() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));
        when(personRepository.findAllByCreatedById(fakeAdmin.getId())).thenReturn(List.of(recipient));

        notificationService.sendToAllUsers("title", "msg", null);

        verify(personRepository, never()).findAll();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NotificationEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<NotificationEntity> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertThat(saved).hasSize(2); // fakeAdmin + recipient
    }

    // ─────────────────────── sendToUsers ───────────────────────
    
    @Test
    @DisplayName("sendToUsers (FAKE_ADMIN) — фільтрує IDs лише на дозволені")
    void sendToUsers_fakeAdmin_filtersRecipients() {
        setSecurityContext("fake@test.com", "ROLE_FAKE_ADMIN");
        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));

        PersonEntity createdUser = new PersonEntity();
        createdUser.setId(UUID.randomUUID());
        createdUser.setCreatedBy(fakeAdmin);

        PersonEntity otherUser = new PersonEntity();
        otherUser.setId(UUID.randomUUID());
        // createdBy null — отже, не дозволений для fakeAdmin

        List<UUID> inputIds = List.of(fakeAdmin.getId(), createdUser.getId(), otherUser.getId());
        when(personRepository.findAllById(inputIds)).thenReturn(List.of(fakeAdmin, createdUser, otherUser));

        notificationService.sendToUsers("title", "msg", inputIds, null);

        // only fakeAdmin and createdUser should be preserved
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NotificationEntity>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(notificationRepository).saveAll(captor.capture());
        List<NotificationEntity> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        assertThat(saved).hasSize(2);
    }

    // ─────────────────────── getUserNotifications ───────────────────────
    
    @Test
    @DisplayName("getUserNotifications — повертає Page")
    void getUserNotifications_success() {
        Page<NotificationEntity> page = new PageImpl<>(List.of(new NotificationEntity()));
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(eq(recipientId), any(PageRequest.class)))
                .thenReturn(page);

        Page<NotificationEntity> result = notificationService.getUserNotifications(recipientId, PageRequest.of(0, 10));
        assertThat(result).isSameAs(page);
    }

    // ─────────────────────── markAsRead / markAsUnread ───────────────────────

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

    // ─────────────────────── bulk operations ───────────────────────

    @Test
    @DisplayName("markAllAsRead — позначає всі як прочитані")
    void markAllAsRead_success() {
        NotificationEntity n1 = new NotificationEntity();
        n1.setRead(false);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)).thenReturn(List.of(n1));

        notificationService.markAllAsRead(recipientId);

        assertThat(n1.isRead()).isTrue();
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("markAllAsUnread — позначає всі як непрочитані")
    void markAllAsUnread_success() {
        NotificationEntity n1 = new NotificationEntity();
        n1.setRead(true);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)).thenReturn(List.of(n1));

        notificationService.markAllAsUnread(recipientId);

        assertThat(n1.isRead()).isFalse();
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("deleteAll — видаляє всі знайдені сповіщення користувача")
    void deleteAll_success() {
        NotificationEntity n1 = new NotificationEntity();
        List<NotificationEntity> list = List.of(n1);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId)).thenReturn(list);

        notificationService.deleteAll(recipientId);

        verify(notificationRepository).deleteAll(list);
    }

    // ─────────────────────── deleteNotification ───────────────────────

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
                
        verify(notificationRepository, never()).deleteById(any());
    }

    // ─────────────────────── getUnreadCount ───────────────────────

    @Test
    @DisplayName("getUnreadCount — повертає кількість непрочитаних")
    void getUnreadCount_returnsValue() {
        when(notificationRepository.countByRecipientIdAndIsReadFalse(recipientId)).thenReturn(5L);

        long count = notificationService.getUnreadCount(recipientId);

        assertThat(count).isEqualTo(5L);
    }
}
