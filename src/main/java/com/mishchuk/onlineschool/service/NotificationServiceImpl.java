package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.repository.NotificationRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.NotificationEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final PersonRepository personRepository;

    @Override
    @Transactional
    public NotificationEntity createNotification(UUID recipientId, String title, String message,
            NotificationType type, String buttonUrl) {
        log.info("Creating notification for user: {}", recipientId);

        PersonEntity recipient = personRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + recipientId));

        NotificationEntity notification = new NotificationEntity();
        notification.setRecipient(recipient);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setButtonUrl(buttonUrl);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void broadcastToAdmins(String title, String message, NotificationType type, String buttonUrl) {
        log.info("Broadcasting notification to all ADMINS: {}", title);
        java.util.List<PersonEntity> admins = personRepository
                .findAllByRole(com.mishchuk.onlineschool.repository.entity.PersonRole.ADMIN);

        java.util.List<NotificationEntity> notifications = admins.stream()
                .map(admin -> {
                    NotificationEntity n = new NotificationEntity();
                    n.setRecipient(admin);
                    n.setTitle(title);
                    n.setMessage(message);
                    n.setType(type);
                    n.setButtonUrl(buttonUrl);
                    n.setRead(false);
                    return n;
                })
                .collect(java.util.stream.Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void sendToAllUsers(String title, String message, String buttonUrl) {
        log.info("Broadcasting notification to ALL users: {}", title);
        java.util.List<PersonEntity> allUsers = personRepository.findAll();

        java.util.List<NotificationEntity> notifications = allUsers.stream()
                .map(user -> {
                    NotificationEntity n = new NotificationEntity();
                    n.setRecipient(user);
                    n.setTitle(title);
                    n.setMessage(message);
                    n.setType(com.mishchuk.onlineschool.repository.entity.NotificationType.ADMIN_ANNOUNCEMENT);
                    n.setButtonUrl(buttonUrl);
                    n.setRead(false);
                    return n;
                })
                .collect(java.util.stream.Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void sendToUsers(String title, String message, java.util.List<UUID> userIds, String buttonUrl) {
        log.info("Sending notification to {} specific users", userIds.size());
        java.util.List<PersonEntity> recipients = personRepository.findAllById(userIds);

        java.util.List<NotificationEntity> notifications = recipients.stream()
                .map(user -> {
                    NotificationEntity n = new NotificationEntity();
                    n.setRecipient(user);
                    n.setTitle(title);
                    n.setMessage(message);
                    n.setType(com.mishchuk.onlineschool.repository.entity.NotificationType.ADMIN_ANNOUNCEMENT);
                    n.setButtonUrl(buttonUrl);
                    n.setRead(false);
                    return n;
                })
                .collect(java.util.stream.Collectors.toList());

        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationEntity> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAsUnread(UUID notificationId) {
        NotificationEntity notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        notification.setRead(false);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        java.util.List<NotificationEntity> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void markAllAsUnread(UUID userId) {
        java.util.List<NotificationEntity> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setRead(false));
        notificationRepository.saveAll(notifications);
    }

    @Override
    @Transactional
    public void deleteAll(UUID userId) {
        java.util.List<NotificationEntity> notifications = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(userId);
        notificationRepository.deleteAll(notifications);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID id) {
        if (!notificationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Notification not found with id: " + id);
        }
        notificationRepository.deleteById(id);
    }
}
