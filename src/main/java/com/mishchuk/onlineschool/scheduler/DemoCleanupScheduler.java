package com.mishchuk.onlineschool.scheduler;

import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.LessonRepository;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoCleanupScheduler {

    private final PersonRepository personRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;

    /**
     * Executes every hour to clean up data created by FAKE_ADMIN or FAKE_USER
     * that is older than 24 hours.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupOldDemoData() {
        log.info("Starting scheduled cleanup of old demo data...");
        OffsetDateTime threshold = OffsetDateTime.now().minusHours(24);
        cleanupDemoDataCreatedBefore(threshold, null);
        log.info("Finished scheduled cleanup of old demo data.");
    }

    /**
     * Instantly cleans up all data created by a specific user.
     * Used mainly for the logout hook.
     * 
     * @param userId The ID of the fake user
     */
    @Transactional
    public void cleanupDataForUser(UUID userId) {
        log.info("Starting instant cleanup for demo user: {}", userId);
        cleanupDemoDataCreatedBefore(OffsetDateTime.now().plusDays(1), userId); // Clean everything up to now
        log.info("Finished instant cleanup for demo user: {}", userId);
    }

    private void cleanupDemoDataCreatedBefore(OffsetDateTime threshold, UUID specificUserId) {
        List<PersonEntity> fakeUsers;

        if (specificUserId != null) {
            fakeUsers = personRepository.findById(specificUserId).stream().toList();
        } else {
            // Find all demo users
            List<PersonEntity> fakeAdmins = personRepository.findAllByRole(PersonRole.FAKE_ADMIN);
            List<PersonEntity> fakeStandardUsers = personRepository.findAllByRole(PersonRole.FAKE_USER);

            fakeUsers = new java.util.ArrayList<>();
            fakeUsers.addAll(fakeAdmins);
            fakeUsers.addAll(fakeStandardUsers);
        }

        int deletedCoursesCount = 0;
        int deletedModulesCount = 0;
        int deletedLessonsCount = 0;

        for (PersonEntity fakeUser : fakeUsers) {
            // 1. Clean up old Courses (Cascade will drop related modules/lessons if set up,
            // but let's be explicit if not)
            List<CourseEntity> oldCourses = courseRepository.findAll().stream()
                    .filter(c -> c.getCreatedBy() != null &&
                            c.getCreatedBy().getId().equals(fakeUser.getId()) &&
                            c.getCreatedAt().isBefore(threshold))
                    .collect(Collectors.toList());

            if (!oldCourses.isEmpty()) {
                courseRepository.deleteAll(oldCourses);
                deletedCoursesCount += oldCourses.size();
            }

            // Note: If orphans exist for modules/lessons (e.g., added to a real course by a
            // fake admin),
            // this is where we would sweep them.
            // For now, depending on cascade, deleting the course deletes its modules and
            // lessons.
            // If they can create standalone modules/lessons, we delete them here.

            var oldModules = moduleRepository.findAll().stream()
                    .filter(m -> m.getCreatedBy() != null &&
                            m.getCreatedBy().getId().equals(fakeUser.getId()) &&
                            m.getCreatedAt().isBefore(threshold))
                    .collect(Collectors.toList());
            if (!oldModules.isEmpty()) {
                moduleRepository.deleteAll(oldModules);
                deletedModulesCount += oldModules.size();
            }

            var oldLessons = lessonRepository.findAll().stream()
                    .filter(l -> l.getCreatedBy() != null &&
                            l.getCreatedBy().getId().equals(fakeUser.getId()) &&
                            l.getCreatedAt().isBefore(threshold))
                    .collect(Collectors.toList());
            if (!oldLessons.isEmpty()) {
                lessonRepository.deleteAll(oldLessons);
                deletedLessonsCount += oldLessons.size();
            }
        }

        log.info("Cleanup summary: Deleted {} courses, {} modules, {} lessons.",
                deletedCoursesCount, deletedModulesCount, deletedLessonsCount);
    }
}
