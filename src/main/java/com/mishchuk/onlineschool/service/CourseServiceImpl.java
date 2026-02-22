package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.exception.BadRequestException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.CourseMapper;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.CourseReviewRequestRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.entity.*;
import com.mishchuk.onlineschool.service.email.EmailService;
import com.mishchuk.onlineschool.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
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
    private final CourseReviewRequestRepository courseReviewRequestRepository;
    private final EmailService emailService;
    private final PersonRepository personRepository;

    @Override
    @Transactional
    public void createCourse(CourseCreateDto dto, MultipartFile coverImage) {
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

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        personRepository.findByEmail(userEmail).ifPresent(entity::setCreatedBy);

        if (coverImage != null && !coverImage.isEmpty()) {
            try {
                byte[] imageData = coverImage.getBytes();
                String averageColor = calculateAverageColor(imageData);

                CourseCoverEntity coverEntity = new CourseCoverEntity();
                coverEntity.setCourse(entity);
                coverEntity.setImageData(imageData);
                coverEntity.setAverageColor(averageColor);

                entity.setCoverImage(coverEntity);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to read cover image", e);
            }
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
    public Optional<byte[]> getCourseCoverImage(UUID id) {
        return courseRepository.findById(id)
                .map(CourseEntity::getCoverImage)
                .map(CourseCoverEntity::getImageData);
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
                        baseDto.coverImageUrl(),
                        enrollment.get().getExpiresAt(),
                        baseDto.averageColor(),
                        baseDto.createdBy());
            }

            return baseDto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void updateCourse(UUID id, CourseUpdateDto dto, MultipartFile coverImage) {
        log.info("Updating course with ID: {}", id);
        if (dto.promotionalDiscountPercentage() != null && dto.promotionalDiscountAmount() != null) {
            throw new BadRequestException(
                    "Cannot set both promotional discount percentage and amount");
        }
        CourseEntity entity = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() == com.mishchuk.onlineschool.repository.entity.PersonRole.FAKE_ADMIN) {
            if (entity.getCreatedBy() == null || !entity.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "FAKE_ADMIN can only modify their own entities.");
            }
        }

        courseMapper.updateEntityFromDto(dto, entity);

        if (dto.promotionalDiscountPercentage() != null) {
            entity.setPromotionalDiscountAmount(null);
        } else if (dto.promotionalDiscountAmount() != null) {
            entity.setPromotionalDiscountPercentage(null);
        }

        if (Boolean.TRUE.equals(dto.deleteCoverImage())) {
            entity.setCoverImage(null);
        } else if (coverImage != null && !coverImage.isEmpty()) {
            try {
                byte[] imageData = coverImage.getBytes();
                String averageColor = calculateAverageColor(imageData);

                CourseCoverEntity coverEntity = entity.getCoverImage();
                if (coverEntity == null) {
                    coverEntity = new CourseCoverEntity();
                    coverEntity.setCourse(entity);
                    entity.setCoverImage(coverEntity);
                }
                coverEntity.setImageData(imageData);
                coverEntity.setAverageColor(averageColor);
            } catch (java.io.IOException e) {
                throw new RuntimeException("Failed to read cover image", e);
            }
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

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (currentUser.getRole() == com.mishchuk.onlineschool.repository.entity.PersonRole.FAKE_ADMIN) {
            if (entity.getCreatedBy() == null || !entity.getCreatedBy().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "FAKE_ADMIN can only delete their own entities.");
            }
        }

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
            LocalDate newExpiryDate = enrollment.getExpiresAt().toLocalDate();
            emailService.sendAccessExtendedEmail(enrollment.getStudent().getEmail(),
                    enrollment.getStudent().getFirstName(),
                    enrollment.getCourse().getName(),
                    newExpiryDate);
        } catch (Exception e) {
            log.error("Failed to send access extension email to user {}", userId, e);
            // Don't fail the transaction if email sending fails
        }
    }

    private String calculateAverageColor(byte[] imageData) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(bis);
            if (image == null)
                return null;

            long sumR = 0, sumG = 0, sumB = 0;
            int width = image.getWidth();
            int height = image.getHeight();
            long totalPixels = width * height;

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = image.getRGB(x, y);
                    sumR += (pixel >> 16) & 0xFF;
                    sumG += (pixel >> 8) & 0xFF;
                    sumB += pixel & 0xFF;
                }
            }

            int avgR = (int) (sumR / totalPixels);
            int avgG = (int) (sumG / totalPixels);
            int avgB = (int) (sumB / totalPixels);

            return String.format("#%02x%02x%02x", avgR, avgG, avgB);
        } catch (Exception e) {
            log.warn("Failed to calculate average color", e);
            return null;
        }
    }
}
