package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.CourseMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.mishchuk.onlineschool.repository.entity.CourseReviewRequestEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final CourseMapper courseMapper;
    private final NotificationService notificationService;
    private final com.mishchuk.onlineschool.repository.CourseReviewRequestRepository courseReviewRequestRepository;
    private final com.mishchuk.onlineschool.service.email.EmailService emailService;

    @Override
    @Transactional
    public void createCourse(CourseCreateDto dto) {
        log.info("Creating new course: {}", dto.name());
        if (dto.promotionalDiscountPercentage() != null && dto.promotionalDiscountAmount() != null) {
            throw new com.mishchuk.onlineschool.exception.BadRequestException(
                    "Cannot set both promotional discount percentage and amount");
        }
        CourseEntity entity = courseMapper.toEntity(dto);

        // Ensure mutual exclusivity
        if (dto.promotionalDiscountPercentage() != null) {
            entity.setPromotionalDiscountAmount(null);
        } else if (dto.promotionalDiscountAmount() != null) {
            entity.setPromotionalDiscountPercentage(null);
        }

        courseRepository.save(entity);
        log.info("Successfully created course with ID: {}", entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CourseDto> getCourse(UUID id) {
        return courseRepository.findById(id)
                .map(courseMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> getAllCourses() {
        return courseRepository.findAll().stream()
                .map(courseMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseDto> getAllCoursesWithEnrollment(UUID userId) {
        List<CourseEntity> allCourses = courseRepository.findAll();
        List<EnrollmentEntity> userEnrollments = enrollmentRepository.findByStudentId(userId);

        return allCourses.stream().map(course -> {
            CourseDto baseDto = courseMapper.toDto(course);

            // Find enrollment for this course
            Optional<EnrollmentEntity> enrollment = userEnrollments.stream()
                    .filter(e -> e.getCourse().getId().equals(course.getId()))
                    .findFirst();

            if (enrollment.isPresent()) {
                log.info("Found enrollment for course {}: ID={}, Status={}, ExpiresAt={}",
                        course.getId(), enrollment.get().getId(), enrollment.get().getStatus(),
                        enrollment.get().getExpiresAt());

                // Create new DTO with enrollment data
                return new CourseDto(
                        baseDto.id(),
                        baseDto.name(),
                        baseDto.description(),
                        baseDto.modulesNumber(),
                        baseDto.lessonsCount(),
                        baseDto.durationMinutes(),
                        baseDto.status(),
                        baseDto.price(),
                        baseDto.discountAmount(),
                        baseDto.discountPercentage(),
                        baseDto.accessDuration(),
                        baseDto.promotionalDiscountPercentage(),
                        baseDto.promotionalDiscountAmount(),
                        baseDto.nextCourseId(),
                        baseDto.nextCourseName(),
                        baseDto.createdAt(),
                        baseDto.updatedAt(),
                        true, // isEnrolled
                        enrollment.get().getCreatedAt(),
                        enrollment.get().getStatus(),
                        enrollment.get().getExpiresAt());
            }

            return baseDto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateCourse(UUID id, CourseUpdateDto dto) {
        log.info("Updating course with ID: {}", id);
        if (dto.promotionalDiscountPercentage() != null && dto.promotionalDiscountAmount() != null) {
            throw new com.mishchuk.onlineschool.exception.BadRequestException(
                    "Cannot set both promotional discount percentage and amount");
        }
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        courseMapper.updateEntityFromDto(dto, entity);

        // Ensure mutual exclusivity in DB: if one is set, clear the other
        if (dto.promotionalDiscountPercentage() != null) {
            entity.setPromotionalDiscountAmount(null);
        } else if (dto.promotionalDiscountAmount() != null) {
            entity.setPromotionalDiscountPercentage(null);
        }

        courseRepository.save(entity);
        log.info("Successfully updated course with ID: {}", id);
    }

    @Override
    @Transactional
    public void deleteCourse(UUID id) {
        log.info("Deleting course with ID: {}", id);
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        courseRepository.delete(entity);
        log.info("Successfully deleted course with ID: {}", id);
    }

    @Override
    @Transactional
    public void extendAccessForReview(UUID userId, UUID courseId, String videoUrl, String originalFilename) {
        log.info("Extending access for review - User: {}, Course: {}, Video: {}", userId, courseId, originalFilename);

        // Find the enrollment to verify it exists
        EnrollmentEntity enrollment = enrollmentRepository.findByStudentIdAndCourseId(userId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found"));

        // Extend access by 31 days from now
        enrollment.setExpiresAt(java.time.OffsetDateTime.now().plusDays(31));
        enrollment.setStatus("ACTIVE");
        enrollmentRepository.save(enrollment);

        log.info("Access extended by 31 days for user {} on course {}", userId, courseId);
        log.info("Review video stored at: {}", videoUrl);

        // Save review request
        CourseReviewRequestEntity reviewRequest = new CourseReviewRequestEntity();
        reviewRequest.setUser(enrollment.getStudent());
        reviewRequest.setCourse(enrollment.getCourse());
        reviewRequest.setVideoUrl(videoUrl);
        reviewRequest.setOriginalFilename(originalFilename);
        reviewRequest.setStatus("APPROVED"); // Auto-approved for now
        courseReviewRequestRepository.save(reviewRequest);

        // Create notification for user
        String message = "Дякуємо за ваш відгук! Доступ до курсу поновлено на 31 день.";
        notificationService.createNotification(userId, "Доступ продовжено", message,
                NotificationType.COURSE_ACCESS_EXTENDED);

        // Notify Admins
        notificationService.broadcastToAdmins(
                "Продовження доступу за відгук",
                "Користувач " + enrollment.getStudent().getFirstName() + " " + enrollment.getStudent().getLastName() +
                        " (" + enrollment.getStudent().getEmail() + ") надіслав відео-відгук для курсу \"" +
                        enrollment.getCourse().getName() + "\". Доступ продовжено автоматично.",
                NotificationType.SYSTEM,
                videoUrl);

        // Send email notification
        try {
            java.time.LocalDate newExpiryDate = enrollment.getExpiresAt().toLocalDate();
            emailService.sendAccessExtendedEmail(enrollment.getStudent().getEmail(),
                    enrollment.getStudent().getFirstName(),
                    enrollment.getCourse().getName(),
                    newExpiryDate);
        } catch (Exception e) {
            log.error("Failed to send access extension email to user {}", userId, e);
            // Don't fail the transaction if email sending fails
        }
    }
}
