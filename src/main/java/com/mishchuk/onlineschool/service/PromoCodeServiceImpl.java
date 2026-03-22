package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.dto.*;
import com.mishchuk.onlineschool.repository.*;
import com.mishchuk.onlineschool.repository.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeServiceImpl implements PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeUsageRepository promoCodeUsageRepository;
    private final PersonRepository personRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<PromoCodeResponseDto> getPaginatedPromoCodes(String search, String sortKey, String sortDir, String statusSort, Pageable pageable) {
        Page<PromoCodeEntity> entities = promoCodeRepository.findPaginatedPromoCodes(search, sortKey, sortDir, statusSort, pageable);
        
        List<PromoCodeResponseDto> dtos = entities.getContent().stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
                
        return new PageImpl<>(dtos, pageable, entities.getTotalElements());
    }

    @Override
    @Transactional
    public PromoCodeResponseDto createPromoCode(PromoCodeCreateDto dto, String currentUsername) {
        PersonEntity admin = personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        if (promoCodeRepository.findByCodeIgnoreCase(dto.getCode()).isPresent()) {
            throw new IllegalArgumentException("Code already exists");
        }

        PromoCodeEntity entity = new PromoCodeEntity();
        entity.setCode(dto.getCode().toUpperCase());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : PromoCodeStatus.ACTIVE);
        entity.setScope(dto.getScope());
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidUntil(dto.getValidUntil());
        entity.setCreatedBy(admin);
        entity.setStatusUpdatedAt(LocalDateTime.now());

        if (dto.getScope() == PromoCodeScope.PERSONAL) {
            if (dto.getTargetPersonIds() == null || dto.getTargetPersonIds().isEmpty()) {
                throw new IllegalArgumentException("Target persons must be provided for personal scope");
            }
            List<PersonEntity> targets = personRepository.findAllById(dto.getTargetPersonIds());
            if (targets.size() != dto.getTargetPersonIds().size()) {
                throw new IllegalArgumentException("Some target persons not found");
            }
            entity.getTargetPersons().addAll(targets);
        }

        if (dto.getDiscounts() != null) {
            for (var d : dto.getDiscounts()) {
                PromoCodeDiscountEntity discountEntity = new PromoCodeDiscountEntity();
                discountEntity.setDiscountType(d.getDiscountType());
                discountEntity.setDiscountValue(d.getDiscountValue());
                discountEntity.setPromoCode(entity);
                if (d.getCourseId() != null) {
                    CourseEntity course = courseRepository.findById(d.getCourseId())
                            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
                    discountEntity.setCourse(course);
                }
                entity.getDiscounts().add(discountEntity);
            }
        }

        PromoCodeEntity saved = promoCodeRepository.save(entity);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PromoCodeCheckResponseDto checkPromoCode(String code, String currentUsername) {
        PromoCodeEntity entity = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Promo code not found"));

        if (entity.getStatus() != PromoCodeStatus.ACTIVE) {
            throw new IllegalArgumentException("Promo code is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (entity.getValidFrom() != null && now.isBefore(entity.getValidFrom())) {
            throw new IllegalArgumentException("Promo code is not valid yet");
        }
        if (entity.getValidUntil() != null && now.isAfter(entity.getValidUntil())) {
            throw new IllegalArgumentException("Promo code has expired");
        }

        PersonEntity user = personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (entity.getScope() == PromoCodeScope.PERSONAL) {
            boolean isAssigned = entity.getTargetPersons().stream()
                    .anyMatch(p -> p.getId().equals(user.getId()));
            if (!isAssigned) {
                throw new IllegalArgumentException("This promo code is personal and not assigned to you");
            }
        }

        if (promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), user.getId())) {
            throw new IllegalArgumentException("You have already used this promo code");
        }

        List<PromoCodeDiscountResponseDto> discounts = entity.getDiscounts().stream()
                .map(this::mapDiscount)
                .collect(Collectors.toList());

        // Build course cards with promo prices
        List<CourseWithPromoDto> courses = new java.util.ArrayList<>();
        java.util.Set<java.util.UUID> addedCourseIds = new java.util.HashSet<>();

        for (PromoCodeDiscountEntity discount : entity.getDiscounts()) {
            if (discount.getCourse() != null) {
                // Specific course discount
                CourseEntity course = discount.getCourse();
                if (!addedCourseIds.contains(course.getId())) {
                    courses.add(buildCourseWithPromo(course, discount));
                    addedCourseIds.add(course.getId());
                }
            } else {
                // All courses discount — fetch all published courses
                List<CourseEntity> allCourses = courseRepository.findAll().stream()
                        .filter(c -> c.getStatus() == CourseStatus.PUBLISHED)
                        .collect(Collectors.toList());
                for (CourseEntity course : allCourses) {
                    if (!addedCourseIds.contains(course.getId())) {
                        courses.add(buildCourseWithPromo(course, discount));
                        addedCourseIds.add(course.getId());
                    }
                }
            }
        }

        return new PromoCodeCheckResponseDto(entity.getCode(), discounts, courses);
    }

    private CourseWithPromoDto buildCourseWithPromo(CourseEntity course, PromoCodeDiscountEntity discount) {
        CourseWithPromoDto dto = new CourseWithPromoDto();
        dto.setCourseId(course.getId());
        dto.setName(course.getName());
        dto.setDescription(course.getDescription());
        dto.setDiscountType(discount.getDiscountType());
        dto.setDiscountValue(discount.getDiscountValue());

        if (course.getCoverImage() != null) {
            dto.setCoverImageUrl("/api/v1/courses/" + course.getId() + "/cover");
            dto.setAverageColor(course.getCoverImage().getAverageColor());
        }

        java.math.BigDecimal originalPrice = course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO;
        dto.setPrice(originalPrice);

        java.math.BigDecimal promoPrice;
        switch (discount.getDiscountType()) {
            case PERCENTAGE:
                promoPrice = originalPrice.multiply(java.math.BigDecimal.ONE.subtract(
                        discount.getDiscountValue().divide(new java.math.BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
                )).max(java.math.BigDecimal.ZERO);
                break;
            case FIXED_AMOUNT:
                promoPrice = originalPrice.subtract(discount.getDiscountValue()).max(java.math.BigDecimal.ZERO);
                break;
            case FIXED_PRICE:
                promoPrice = discount.getDiscountValue();
                break;
            default:
                promoPrice = originalPrice;
        }
        dto.setPromoPrice(promoPrice.setScale(2, java.math.RoundingMode.HALF_UP));

        // Module and lesson counts
        int modulesCount = course.getModules() != null ? course.getModules().size() : 0;
        int lessonsCount = course.getModules() != null
                ? course.getModules().stream().mapToInt(m -> m.getLessonsNumber() != null ? m.getLessonsNumber() : 0).sum()
                : 0;
        dto.setModulesCount(modulesCount);
        dto.setLessonsCount(lessonsCount);

        return dto;
    }

    @Override
    @Transactional
    public void usePromoCode(String code, UUID courseId, String currentUsername) {
        PromoCodeEntity entity = promoCodeRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> new IllegalArgumentException("Promo code not found"));

        if (entity.getStatus() != PromoCodeStatus.ACTIVE) {
            throw new IllegalArgumentException("Promo code is not active");
        }

        LocalDateTime now = LocalDateTime.now();
        if (entity.getValidFrom() != null && now.isBefore(entity.getValidFrom())) {
            throw new IllegalArgumentException("Promo code is not valid yet");
        }
        if (entity.getValidUntil() != null && now.isAfter(entity.getValidUntil())) {
            throw new IllegalArgumentException("Promo code has expired");
        }

        PersonEntity user = personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (entity.getScope() == PromoCodeScope.PERSONAL) {
            boolean isAssigned = entity.getTargetPersons().stream()
                    .anyMatch(p -> p.getId().equals(user.getId()));
            if (!isAssigned) {
                throw new IllegalArgumentException("This promo code is personal and not assigned to you");
            }
        }

        if (promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), user.getId())) {
            throw new IllegalArgumentException("You have already used this promo code");
        }

        CourseEntity course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));

        // Check if already enrolled
        if (enrollmentRepository.findByStudentIdAndCourseId(user.getId(), courseId).isPresent()) {
            throw new IllegalArgumentException("You are already enrolled in this course");
        }

        // Find matching discount (specific course or "all courses")
        PromoCodeDiscountEntity matchingDiscount = entity.getDiscounts().stream()
                .filter(d -> d.getCourse() == null || d.getCourse().getId().equals(courseId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No discount found for this course"));

        // Calculate prices
        java.math.BigDecimal originalPrice = course.getPrice() != null ? course.getPrice() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal finalPrice;
        switch (matchingDiscount.getDiscountType()) {
            case PERCENTAGE:
                finalPrice = originalPrice.multiply(java.math.BigDecimal.ONE.subtract(
                        matchingDiscount.getDiscountValue().divide(new java.math.BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
                )).max(java.math.BigDecimal.ZERO);
                break;
            case FIXED_AMOUNT:
                finalPrice = originalPrice.subtract(matchingDiscount.getDiscountValue()).max(java.math.BigDecimal.ZERO);
                break;
            case FIXED_PRICE:
                finalPrice = matchingDiscount.getDiscountValue();
                break;
            default:
                finalPrice = originalPrice;
        }

        // Enroll the user (same as normal course purchase)
        enrollmentService.createEnrollment(
                new com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto(user.getId(), courseId)
        );

        // Record promo code usage with price audit trail
        PromoCodeUsageEntity usage = new PromoCodeUsageEntity();
        usage.setPromoCode(entity);
        usage.setPerson(user);
        usage.setCourse(course);
        usage.setDiscountType(matchingDiscount.getDiscountType());
        usage.setDiscountValue(matchingDiscount.getDiscountValue());
        usage.setOriginalPrice(originalPrice);
        usage.setFinalPrice(finalPrice.setScale(2, java.math.RoundingMode.HALF_UP));
        promoCodeUsageRepository.save(usage);
    }

    @Override
    @Transactional
    public PromoCodeResponseDto updatePromoCode(UUID id, PromoCodeCreateDto dto, String currentUsername) {
        // verify admin
        personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));

        PromoCodeEntity entity = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promo code not found"));

        if (!entity.getCode().equalsIgnoreCase(dto.getCode())) {
            if (promoCodeRepository.findByCodeIgnoreCase(dto.getCode()).isPresent()) {
                throw new IllegalArgumentException("Code already exists");
            }
        }

        entity.setCode(dto.getCode().toUpperCase());
        if (dto.getStatus() != null && entity.getStatus() != dto.getStatus()) {
            entity.setStatus(dto.getStatus());
            entity.setStatusUpdatedAt(LocalDateTime.now());
        }
        entity.setScope(dto.getScope());
        entity.setValidFrom(dto.getValidFrom());
        entity.setValidUntil(dto.getValidUntil());

        if (dto.getScope() == PromoCodeScope.PERSONAL) {
            if (dto.getTargetPersonIds() == null || dto.getTargetPersonIds().isEmpty()) {
                throw new IllegalArgumentException("Target persons must be provided for personal scope");
            }
            List<PersonEntity> targets = personRepository.findAllById(dto.getTargetPersonIds());
            if (targets.size() != dto.getTargetPersonIds().size()) {
                throw new IllegalArgumentException("Some target persons not found");
            }
            entity.getTargetPersons().clear();
            entity.getTargetPersons().addAll(targets);
        } else {
            entity.getTargetPersons().clear();
        }

        entity.getDiscounts().clear();

        if (dto.getDiscounts() != null) {
            for (var d : dto.getDiscounts()) {
                PromoCodeDiscountEntity discountEntity = new PromoCodeDiscountEntity();
                discountEntity.setDiscountType(d.getDiscountType());
                discountEntity.setDiscountValue(d.getDiscountValue());
                discountEntity.setPromoCode(entity);
                if (d.getCourseId() != null) {
                    CourseEntity course = courseRepository.findById(d.getCourseId())
                            .orElseThrow(() -> new IllegalArgumentException("Course not found"));
                    discountEntity.setCourse(course);
                }
                entity.getDiscounts().add(discountEntity);
            }
        }

        PromoCodeEntity saved = promoCodeRepository.save(entity);
        return mapToResponseDto(saved);
    }

    @Override
    @Transactional
    public void deletePromoCode(UUID id, String currentUsername) {
        personRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
                
        PromoCodeEntity entity = promoCodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Promo code not found"));
                
        promoCodeRepository.delete(entity);
    }

    private PromoCodeResponseDto mapToResponseDto(PromoCodeEntity entity) {
        PromoCodeResponseDto dto = new PromoCodeResponseDto();
        dto.setId(entity.getId());
        dto.setCode(entity.getCode());
        dto.setStatus(entity.getStatus());
        dto.setScope(entity.getScope());
        if (entity.getTargetPersons() != null && !entity.getTargetPersons().isEmpty()) {
            List<PromoCodeResponseDto.PromoCodeTargetUserDto> targetDtos = entity.getTargetPersons().stream().map(p -> {
                PromoCodeResponseDto.PromoCodeTargetUserDto u = new PromoCodeResponseDto.PromoCodeTargetUserDto();
                u.setId(p.getId());
                u.setName(p.getFirstName() + (p.getLastName() != null ? " " + p.getLastName() : ""));
                u.setEmail(p.getEmail());
                u.setPhone(p.getPhoneNumber());

                if (entity.getUsages() != null) {
                    List<UUID> usedCourses = entity.getUsages().stream()
                            .filter(usage -> usage.getPerson() != null && usage.getPerson().getId().equals(p.getId()))
                            .filter(usage -> usage.getCourse() != null)
                            .map(usage -> usage.getCourse().getId())
                            .collect(Collectors.toList());
                    u.setUsedCourseIds(usedCourses);
                }

                return u;
            }).collect(Collectors.toList());
            dto.setTargetPersons(targetDtos);
        }
        dto.setValidFrom(entity.getValidFrom());
        dto.setValidUntil(entity.getValidUntil());
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm");
        LocalDateTime now = LocalDateTime.now();

        if (entity.getValidUntil() == null) {
            dto.setValidUntilDisplay("Безстроково");
        } else {
            dto.setValidUntilDisplay(entity.getValidUntil().format(formatter));
        }

        boolean isPending = entity.getStatus() == PromoCodeStatus.INACTIVE && 
                            entity.getValidFrom() != null && 
                            entity.getValidFrom().isAfter(now);
        dto.setPendingActivation(isPending);

        if (isPending) {
            dto.setValidFromDisplay(entity.getValidFrom().format(formatter));
        } else if (entity.getStatus() == PromoCodeStatus.INACTIVE) {
            dto.setValidFromDisplay("—");
        } else {
            LocalDateTime activeSince = entity.getStatusUpdatedAt() != null ? entity.getStatusUpdatedAt() : entity.getCreatedAt();
            dto.setValidFromDisplay(activeSince.format(formatter));
        }
        
        if (entity.getDiscounts() != null) {
            dto.setDiscounts(entity.getDiscounts().stream().map(this::mapDiscount).collect(Collectors.toList()));
        }
        return dto;
    }

    private PromoCodeDiscountResponseDto mapDiscount(PromoCodeDiscountEntity discountEntity) {
        PromoCodeDiscountResponseDto dto = new PromoCodeDiscountResponseDto();
        if (discountEntity.getCourse() != null) {
            dto.setCourseId(discountEntity.getCourse().getId());
            dto.setCourseName(discountEntity.getCourse().getName());
            dto.setOriginalCoursePrice(discountEntity.getCourse().getPrice());
        } else {
            dto.setCourseName("Усі курси");
        }
        dto.setDiscountType(discountEntity.getDiscountType());
        dto.setDiscountValue(discountEntity.getDiscountValue());
        return dto;
    }

    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void activateScheduledPromoCodes() {
        LocalDateTime now = LocalDateTime.now();
        List<PromoCodeEntity> inactiveCodes = promoCodeRepository.findByStatusAndValidFromLessThanEqual(PromoCodeStatus.INACTIVE, now);

        for (PromoCodeEntity pc : inactiveCodes) {
            pc.setStatus(PromoCodeStatus.ACTIVE);
            pc.setStatusUpdatedAt(now);
            log.info("Automatically activated promo code: {}", pc.getCode());
        }
        if (!inactiveCodes.isEmpty()) {
            promoCodeRepository.saveAll(inactiveCodes);
        }
    }
}
