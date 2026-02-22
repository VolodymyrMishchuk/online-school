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
        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<CourseEntity> allCourses = courseRepository.findAll();

        if (currentUser.getRole() == com.mishchuk.onlineschool.repository.entity.PersonRole.USER) {
            allCourses = allCourses.stream()
                    .filter(c -> c.getStatus() == com.mishchuk.onlineschool.repository.entity.CourseStatus.PUBLISHED)
                    .collect(Collectors.toList());
        }

        return allCourses.stream()
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
                        baseDto.version(),
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

    @Override
    @Transactional
    public void cloneCourse(UUID id) {
        log.info("Cloning course with ID: {}", id);
        CourseEntity originalCourse = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + id));

        String userEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        PersonEntity currentUser = personRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        CourseEntity clonedCourse = new CourseEntity();
        clonedCourse.setName(originalCourse.getName() + " (Copy)");
        clonedCourse.setDescription(originalCourse.getDescription());
        clonedCourse.setAccessDuration(originalCourse.getAccessDuration());
        clonedCourse.setPrice(originalCourse.getPrice());
        clonedCourse.setDiscountAmount(originalCourse.getDiscountAmount());
        clonedCourse.setDiscountPercentage(originalCourse.getDiscountPercentage());
        clonedCourse.setPromotionalDiscountAmount(originalCourse.getPromotionalDiscountAmount());
        clonedCourse.setPromotionalDiscountPercentage(originalCourse.getPromotionalDiscountPercentage());
        clonedCourse.setModulesNumber(originalCourse.getModulesNumber());
        clonedCourse.setStatus(com.mishchuk.onlineschool.repository.entity.CourseStatus.DRAFT);

        String originalVersion = originalCourse.getVersion() != null ? originalCourse.getVersion() : "1.0";
        try {
            int dotIndex = originalVersion.indexOf('.');
            if (dotIndex != -1) {
                int major = Integer.parseInt(originalVersion.substring(0, dotIndex));
                clonedCourse.setVersion((major + 1) + ".0");
            } else {
                int major = Integer.parseInt(originalVersion);
                clonedCourse.setVersion((major + 1) + ".0");
            }
        } catch (NumberFormatException e) {
            clonedCourse.setVersion(originalVersion + " (Copy)"); // Polyfill just in case
        }

        clonedCourse.setCreatedBy(currentUser);
        clonedCourse.setNextCourse(originalCourse.getNextCourse());

        if (originalCourse.getCoverImage() != null) {
            CourseCoverEntity clonedCover = new CourseCoverEntity();
            clonedCover.setImageData(originalCourse.getCoverImage().getImageData());
            clonedCover.setAverageColor(originalCourse.getCoverImage().getAverageColor());
            clonedCover.setCourse(clonedCourse);
            clonedCourse.setCoverImage(clonedCover);
        }

        if (originalCourse.getModules() != null) {
            List<ModuleEntity> clonedModules = new java.util.ArrayList<>();
            for (ModuleEntity originalModule : originalCourse.getModules()) {
                ModuleEntity clonedModule = new ModuleEntity();
                clonedModule.setCourse(clonedCourse);
                clonedModule.setName(originalModule.getName());
                clonedModule.setDescription(originalModule.getDescription());
                clonedModule.setLessonsNumber(originalModule.getLessonsNumber());
                clonedModule.setCreatedBy(currentUser);

                if (originalModule.getLessons() != null) {
                    List<LessonEntity> clonedLessons = new java.util.ArrayList<>();
                    for (LessonEntity originalLesson : originalModule.getLessons()) {
                        LessonEntity clonedLesson = new LessonEntity();
                        clonedLesson.setModule(clonedModule);
                        clonedLesson.setName(originalLesson.getName());
                        clonedLesson.setDescription(originalLesson.getDescription());
                        clonedLesson.setVideoUrl(originalLesson.getVideoUrl());
                        clonedLesson.setDurationMinutes(originalLesson.getDurationMinutes());
                        clonedLesson.setCreatedBy(currentUser);

                        if (originalLesson.getFiles() != null) {
                            List<com.mishchuk.onlineschool.repository.entity.FileEntity> clonedFiles = new java.util.ArrayList<>();
                            for (com.mishchuk.onlineschool.repository.entity.FileEntity originalFile : originalLesson
                                    .getFiles()) {
                                com.mishchuk.onlineschool.repository.entity.FileEntity clonedFile = new com.mishchuk.onlineschool.repository.entity.FileEntity();
                                clonedFile.setLesson(clonedLesson);
                                clonedFile.setFileName(originalFile.getFileName());
                                clonedFile.setOriginalName(originalFile.getOriginalName());
                                clonedFile.setContentType(originalFile.getContentType());
                                clonedFile.setFileSize(originalFile.getFileSize());
                                clonedFile.setMinioObjectName(originalFile.getMinioObjectName());
                                clonedFile.setBucketName(originalFile.getBucketName());
                                clonedFile.setUploadedBy(currentUser);
                                clonedFile.setRelatedEntityType(originalFile.getRelatedEntityType());
                                clonedFile.setRelatedEntityId(originalFile.getRelatedEntityId());
                                clonedFiles.add(clonedFile);
                            }
                            clonedLesson.setFiles(clonedFiles);
                        }

                        clonedLessons.add(clonedLesson);
                    }
                    clonedModule.setLessons(clonedLessons);
                }

                clonedModules.add(clonedModule);
            }
            clonedCourse.setModules(clonedModules);
        }

        courseRepository.save(clonedCourse);
        log.info("Successfully cloned course to new ID: {}", clonedCourse.getId());
    }

    @Override
    @Transactional
    public void updateCourseStatus(UUID id, com.mishchuk.onlineschool.repository.entity.CourseStatus status) {
        log.info("Updating status for course {} to {}", id, status);
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

        entity.setStatus(status);
        courseRepository.save(entity);
        log.info("Successfully updated status for course {}", id);
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
