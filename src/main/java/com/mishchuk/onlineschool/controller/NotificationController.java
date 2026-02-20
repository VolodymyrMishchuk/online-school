package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.NotificationDto;
import com.mishchuk.onlineschool.repository.entity.NotificationEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final UserDetailsService userDetailsService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getUserNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (principal == null) {
            return ResponseEntity.status(401).build();
        }

        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<NotificationEntity> notifications = notificationService.getUserNotifications(person.getId(), pageable);

        List<NotificationDto> dtos = notifications.getContent().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID id,
            Principal principal) {

        // In a real app we might verify ownership here
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/unread")
    public ResponseEntity<Void> markAsUnread(
            @PathVariable UUID id,
            Principal principal) {

        // In a real app we might verify ownership here
        notificationService.markAsUnread(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
        return ResponseEntity.ok(notificationService.getUnreadCount(person.getId()));
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
        notificationService.markAllAsRead(person.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/unread-all")
    public ResponseEntity<Void> markAllAsUnread(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
        notificationService.markAllAsUnread(person.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/all")
    public ResponseEntity<Void> deleteAll(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
        notificationService.deleteAll(person.getId());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        // In a real app we might verify ownership here (that the notification belongs
        // to the user)
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcastToAll(
            @RequestBody @jakarta.validation.Valid com.mishchuk.onlineschool.controller.dto.BroadcastRequest request,
            Principal principal) {
        // Add Admin check
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
        if (!person.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        notificationService.sendToAllUsers(request.title(), request.message(), request.buttonUrl());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/send-to-users")
    public ResponseEntity<Void> sendToUsers(
            @RequestBody @jakarta.validation.Valid com.mishchuk.onlineschool.controller.dto.TargetedNotificationRequest request,
            Principal principal) {
        // Add Admin check
        PersonEntity person = ((CustomUserDetailsService) userDetailsService).getPerson(principal.getName());
        if (!person.getRole().name().equals("ADMIN")) {
            return ResponseEntity.status(403).build();
        }

        notificationService.sendToUsers(request.title(), request.message(), request.userIds(), request.buttonUrl());
        return ResponseEntity.ok().build();
    }

    private NotificationDto toDto(NotificationEntity entity) {
        return new NotificationDto(
                entity.getId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getType(),
                entity.isRead(),
                entity.getCreatedAt(),
                entity.getButtonUrl());
    }
}
