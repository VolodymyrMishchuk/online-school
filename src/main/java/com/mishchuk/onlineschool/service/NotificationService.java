package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.repository.entity.NotificationEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationService {
    NotificationEntity createNotification(UUID recipientId, String title, String message, NotificationType type,
            String buttonUrl);

    // Overload for backward compatibility if needed, or just update all calls
    default NotificationEntity createNotification(UUID recipientId, String title, String message,
            NotificationType type) {
        return createNotification(recipientId, title, message, type, null);
    }

    void broadcastToAdmins(String title, String message, NotificationType type, String buttonUrl);

    default void broadcastToAdmins(String title, String message, NotificationType type) {
        broadcastToAdmins(title, message, type, null);
    }

    void sendToAllUsers(String title, String message, String buttonUrl);

    default void sendToAllUsers(String title, String message) {
        sendToAllUsers(title, message, null);
    }

    void sendToUsers(String title, String message, java.util.List<UUID> userIds, String buttonUrl);

    default void sendToUsers(String title, String message, java.util.List<UUID> userIds) {
        sendToUsers(title, message, userIds, null);
    }

    Page<NotificationEntity> getUserNotifications(UUID userId, Pageable pageable);

    void markAsRead(UUID notificationId);

    void markAsUnread(UUID notificationId);

    void markAllAsRead(UUID userId);

    void markAllAsUnread(UUID userId);

    void deleteAll(UUID userId);

    long getUnreadCount(UUID userId);

    void deleteNotification(UUID id);
}
