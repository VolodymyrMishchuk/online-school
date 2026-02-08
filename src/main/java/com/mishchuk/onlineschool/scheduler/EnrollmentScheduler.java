package com.mishchuk.onlineschool.scheduler;

import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EnrollmentScheduler {

    private final EnrollmentRepository enrollmentRepository;

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void expireEnrollments() {
        log.info("Starting enrollment expiration check...");
        List<EnrollmentEntity> activeEnrollments = enrollmentRepository.findByStatus("ACTIVE");

        int expiredCount = 0;
        OffsetDateTime now = OffsetDateTime.now();

        for (EnrollmentEntity enrollment : activeEnrollments) {
            if (enrollment.getCourse() != null && enrollment.getCourse().getAccessDuration() != null) {
                OffsetDateTime expirationDate = enrollment.getCreatedAt()
                        .plusDays(enrollment.getCourse().getAccessDuration());

                if (now.isAfter(expirationDate)) {
                    enrollment.setStatus("BLOCKED");
                    enrollmentRepository.save(enrollment);
                    expiredCount++;
                }
            }
        }

        log.info("Enrollment expiration check completed. Blocked {} enrollments.", expiredCount);
    }
}
