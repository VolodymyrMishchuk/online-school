package com.mishchuk.onlineschool.scheduler;

import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class CourseExpirationReminderScheduler {

    private final EnrollmentRepository enrollmentRepository;
    private final EmailService emailService;

    @Scheduled(cron = "0 0 9 * * *") // Run every day at 9:00 AM
    @Transactional
    public void sendExpirationReminders() {
        log.info("Starting course expiration reminder check...");

        List<EnrollmentEntity> activeEnrollments = enrollmentRepository.findByStatus("ACTIVE");
        int remindersSent = 0;

        LocalDate today = LocalDate.now();

        for (EnrollmentEntity enrollment : activeEnrollments) {
            if (enrollment.getCourse().getAccessDuration() == null) {
                continue;
            }

            // Calculate expiration date
            // created_at + access_duration (days) = expiration_date
            OffsetDateTime createdAt = enrollment.getCreatedAt();
            int accessDuration = enrollment.getCourse().getAccessDuration();
            LocalDate expirationDate = createdAt.plusDays(accessDuration).toLocalDate();

            // Check if expiration is exactly 30 days from now
            // expiration_date - 30 days = today
            if (expirationDate.minusDays(30).isEqual(today)) {
                try {
                    emailService.sendCourseExpirationReminderEmail(
                            enrollment.getStudent().getEmail(),
                            enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName(),
                            enrollment.getCourse().getName(),
                            expirationDate);
                    remindersSent++;
                } catch (Exception e) {
                    log.error("Failed to send expiration reminder for enrollment {}", enrollment.getId(), e);
                }
            }
        }

        log.info("Course expiration reminder check completed. Sent {} reminders.", remindersSent);
    }
}
