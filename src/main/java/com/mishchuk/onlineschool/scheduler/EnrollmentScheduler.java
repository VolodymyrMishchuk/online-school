package com.mishchuk.onlineschool.scheduler;

import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.mishchuk.onlineschool.service.NotificationService;
import com.mishchuk.onlineschool.service.email.EmailService;
import com.mishchuk.onlineschool.repository.entity.NotificationType;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentScheduler {

    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void expireEnrollments() {
        log.info("Starting enrollment expiration check...");
        List<EnrollmentEntity> activeEnrollments = enrollmentRepository.findByStatus("ACTIVE");

        int expiredCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (EnrollmentEntity enrollment : activeEnrollments) {
            if (enrollment.getCourse() != null && enrollment.getCourse().getAccessDuration() != null) {
                OffsetDateTime expirationDate;

                if (enrollment.getExpiresAt() != null) {
                    expirationDate = enrollment.getExpiresAt();
                } else {
                    expirationDate = enrollment.getCreatedAt()
                            .plusDays(enrollment.getCourse().getAccessDuration());
                }

                if (now.isAfter(expirationDate)) {
                    enrollment.setStatus("BLOCKED");
                    enrollmentRepository.save(enrollment);
                    expiredCount++;
                }
            }
        }

        log.info("Enrollment expiration check completed. Blocked {} enrollments.", expiredCount);
    }

    @Scheduled(cron = "0 0 10 * * *") // Every day at 10 AM
    @Transactional
    public void notifyExpiringEnrollments() {
        log.info("Starting check for enrollments expiring in 30 days...");
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime targetStart = now.plusDays(30).withHour(0).withMinute(0).withSecond(0).withNano(0);
        OffsetDateTime targetEnd = targetStart.plusDays(1).minusNanos(1);

        List<EnrollmentEntity> expiringEnrollments = enrollmentRepository.findByStatusAndExpiresAtBetween(
                "ACTIVE", targetStart, targetEnd);

        int notifiedCount = 0;

        for (EnrollmentEntity enrollment : expiringEnrollments) {
            if (enrollment.getStudent() != null && enrollment.getCourse() != null) {
                String courseName = enrollment.getCourse().getName();
                java.time.LocalDate expiryDate = enrollment.getExpiresAt().toLocalDate();

                // In-app notification
                String message = "Доступ до курсу " + courseName + " завершується через місяць (" + expiryDate + ").";
                notificationService.createNotification(
                        enrollment.getStudent().getId(),
                        "Завершення доступу до курсу",
                        message,
                        NotificationType.COURSE_EXPIRING);

                // Email notification
                try {
                    emailService.sendCourseExpirationReminderEmail(
                            enrollment.getStudent().getEmail(),
                            enrollment.getStudent().getFirstName(),
                            courseName,
                            expiryDate);
                } catch (Exception e) {
                    log.error("Failed to send expiration email to {}", enrollment.getStudent().getEmail(), e);
                }
                notifiedCount++;
            }
        }

        log.info("Expiration notification check completed. Sent {} notifications.", notifiedCount);
    }
}
