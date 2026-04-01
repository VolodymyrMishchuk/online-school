package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.dto.PromoCodeCreateDto;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.PromoCodeRepository;
import com.mishchuk.onlineschool.repository.PromoCodeUsageRepository;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.DiscountType;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PromoCodeDiscountEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import com.mishchuk.onlineschool.repository.entity.PromoCodeUsageEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromoCodeServiceImplTest {

    @Mock private PromoCodeRepository promoCodeRepository;
    @Mock private PromoCodeUsageRepository promoCodeUsageRepository;
    @Mock private PersonRepository personRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private EnrollmentService enrollmentService;
    @Mock private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private PromoCodeServiceImpl promoCodeService;

    private PersonEntity admin;
    private PersonEntity fakeAdmin;
    private PersonEntity regularUser;

    @BeforeEach
    void setUp() {
        admin = new PersonEntity();
        admin.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        admin.setEmail("admin@test.com");
        admin.setRole(PersonRole.ADMIN);

        fakeAdmin = new PersonEntity();
        fakeAdmin.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        fakeAdmin.setEmail("fake@test.com");
        fakeAdmin.setRole(PersonRole.FAKE_ADMIN);

        regularUser = new PersonEntity();
        regularUser.setId(UUID.fromString("00000000-0000-0000-0000-000000000003"));
        regularUser.setEmail("user@test.com");
        regularUser.setRole(PersonRole.USER);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ─────────────────────── createPromoCode ───────────────────────

    @Test
    @DisplayName("createPromoCode — ADMIN успішно створює GLOBAL промокод")
    void createPromoCode_admin_global_success() {
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findByCodeIgnoreCase("SPRING25")).thenReturn(Optional.empty());

        PromoCodeEntity saved = promoCodeEntity("SPRING25");
        when(promoCodeRepository.save(any(PromoCodeEntity.class))).thenReturn(saved);

        promoCodeService.createPromoCode(buildDto("SPRING25", PromoCodeScope.GLOBAL, null), "admin@test.com");

        ArgumentCaptor<PromoCodeEntity> captor = ArgumentCaptor.forClass(PromoCodeEntity.class);
        verify(promoCodeRepository).save(captor.capture());

        PromoCodeEntity captured = captor.getValue();
        assertThat(captured.getCode()).isEqualTo("SPRING25");
        assertThat(captured.getCreatedBy()).isSameAs(admin);
        assertThat(captured.getStatus()).isEqualTo(PromoCodeStatus.ACTIVE);
    }

    @Test
    @DisplayName("createPromoCode — кидає IllegalArgumentException якщо код вже існує")
    void createPromoCode_duplicateCode_throws() {
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findByCodeIgnoreCase("EXISTING")).thenReturn(Optional.of(promoCodeEntity("EXISTING")));

        assertThatThrownBy(() -> promoCodeService.createPromoCode(
                buildDto("EXISTING", PromoCodeScope.GLOBAL, null), "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPromoCode — кидає IllegalArgumentException для PERSONAL без targetPersonIds")
    void createPromoCode_personalNoTargets_throws() {
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findByCodeIgnoreCase("PERS25")).thenReturn(Optional.empty());

        PromoCodeCreateDto dto = buildDto("PERS25", PromoCodeScope.PERSONAL, null);
        dto.setTargetPersonIds(null);

        assertThatThrownBy(() -> promoCodeService.createPromoCode(dto, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target persons");

        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPromoCode — PERSONAL з validPersonIds успішно створюється")
    void createPromoCode_personal_withTargets_success() {
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findByCodeIgnoreCase("VIPCODE")).thenReturn(Optional.empty());

        PromoCodeCreateDto dto = buildDto("VIPCODE", PromoCodeScope.PERSONAL, null);
        dto.setTargetPersonIds(Set.of(regularUser.getId()));

        when(personRepository.findAllById(Set.of(regularUser.getId())))
                .thenReturn(List.of(regularUser));

        PromoCodeEntity saved = promoCodeEntity("VIPCODE");
        when(promoCodeRepository.save(any())).thenReturn(saved);

        promoCodeService.createPromoCode(dto, "admin@test.com");

        verify(promoCodeRepository).save(any());
    }

    @Test
    @DisplayName("createPromoCode — кидає IllegalArgumentException якщо деякі target persons не знайдені")
    void createPromoCode_personalMissingPersons_throws() {
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findByCodeIgnoreCase("PERS50")).thenReturn(Optional.empty());

        UUID missingId = UUID.randomUUID();
        PromoCodeCreateDto dto = buildDto("PERS50", PromoCodeScope.PERSONAL, null);
        dto.setTargetPersonIds(Set.of(regularUser.getId(), missingId));

        // findAllById повертає тільки одного з двох
        when(personRepository.findAllById(any())).thenReturn(List.of(regularUser));

        assertThatThrownBy(() -> promoCodeService.createPromoCode(dto, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Some target persons not found");

        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("createPromoCode — стандартний статус ACTIVE якщо dto.status = null")
    void createPromoCode_nullStatus_defaultsToActive() {
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findByCodeIgnoreCase("DEFAULT")).thenReturn(Optional.empty());

        PromoCodeCreateDto dto = buildDto("DEFAULT", PromoCodeScope.GLOBAL, null);
        dto.setStatus(null);

        PromoCodeEntity saved = promoCodeEntity("DEFAULT");
        when(promoCodeRepository.save(any())).thenReturn(saved);

        promoCodeService.createPromoCode(dto, "admin@test.com");

        ArgumentCaptor<PromoCodeEntity> captor = ArgumentCaptor.forClass(PromoCodeEntity.class);
        verify(promoCodeRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(PromoCodeStatus.ACTIVE);
    }

    // ─────────────────────── checkPromoCode ───────────────────────

    @Test
    @DisplayName("checkPromoCode — успішна перевірка активного, невикористаного GLOBAL промокоду")
    void checkPromoCode_active_notUsed_global_success() {
        PromoCodeEntity entity = promoCodeEntity("VALID10");
        when(promoCodeRepository.findByCodeIgnoreCase("VALID10")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), regularUser.getId()))
                .thenReturn(false);

        promoCodeService.checkPromoCode("VALID10", "user@test.com");
    }

    @Test
    @DisplayName("checkPromoCode — кидає якщо промокод INACTIVE")
    void checkPromoCode_inactive_throws() {
        PromoCodeEntity entity = promoCodeEntity("OFF50");
        entity.setStatus(PromoCodeStatus.INACTIVE);

        when(promoCodeRepository.findByCodeIgnoreCase("OFF50")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("OFF50", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("checkPromoCode — кидає якщо validUntil вже минув")
    void checkPromoCode_expired_throws() {
        PromoCodeEntity entity = promoCodeEntity("EXPIRED");
        entity.setValidUntil(LocalDateTime.now().minusDays(1));

        when(promoCodeRepository.findByCodeIgnoreCase("EXPIRED")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("EXPIRED", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("checkPromoCode — кидає якщо validFrom ще не настав")
    void checkPromoCode_notYetValid_throws() {
        PromoCodeEntity entity = promoCodeEntity("FUTURE");
        entity.setValidFrom(LocalDateTime.now().plusDays(3));

        when(promoCodeRepository.findByCodeIgnoreCase("FUTURE")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("FUTURE", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not valid yet");
    }

    @Test
    @DisplayName("checkPromoCode — кидає якщо вже використано")
    void checkPromoCode_alreadyUsed_throws() {
        PromoCodeEntity entity = promoCodeEntity("USED10");

        when(promoCodeRepository.findByCodeIgnoreCase("USED10")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), regularUser.getId()))
                .thenReturn(true);

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("USED10", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used");
    }

    @Test
    @DisplayName("checkPromoCode — PERSONAL промокод кидає якщо user не призначений")
    void checkPromoCode_personal_notAssigned_throws() {
        PromoCodeEntity entity = promoCodeEntity("VIPONLY");
        entity.setScope(PromoCodeScope.PERSONAL);
        entity.setTargetPersons(new HashSet<>());

        when(promoCodeRepository.findByCodeIgnoreCase("VIPONLY")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("VIPONLY", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("personal and not assigned");
    }

    @Test
    @DisplayName("checkPromoCode — промокод не знайдено кидає IllegalArgumentException")
    void checkPromoCode_notFound_throws() {
        when(promoCodeRepository.findByCodeIgnoreCase("GHOST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("GHOST", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }

    // ─────────────────────── usePromoCode ───────────────────────

    @Test
    @DisplayName("usePromoCode — успішно зараховує користувача та записує usage")
    void usePromoCode_success() {
        UUID courseId = UUID.fromString("00000000-0000-0000-0000-000000000010");
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        course.setPrice(new BigDecimal("100.00"));

        PromoCodeEntity entity = promoCodeEntity("SAVE20");

        PromoCodeDiscountEntity discount = new PromoCodeDiscountEntity();
        discount.setDiscountType(DiscountType.PERCENTAGE);
        discount.setDiscountValue(new BigDecimal("20"));
        discount.setCourse(null);
        entity.getDiscounts().add(discount);

        when(promoCodeRepository.findByCodeIgnoreCase("SAVE20")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), regularUser.getId()))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), courseId))
                .thenReturn(Optional.empty());

        promoCodeService.usePromoCode("SAVE20", courseId, "user@test.com");

        verify(enrollmentService).createEnrollment(any(EnrollmentCreateDto.class));

        ArgumentCaptor<PromoCodeUsageEntity> usageCaptor = ArgumentCaptor.forClass(PromoCodeUsageEntity.class);
        verify(promoCodeUsageRepository).save(usageCaptor.capture());
        PromoCodeUsageEntity usage = usageCaptor.getValue();
        assertThat(usage.getPerson()).isSameAs(regularUser);
        assertThat(usage.getCourse()).isSameAs(course);
        assertThat(usage.getOriginalPrice()).isEqualByComparingTo("100.00");
        assertThat(usage.getFinalPrice()).isEqualByComparingTo("80.00");
    }

    @Test
    @DisplayName("usePromoCode — кидає якщо вже зарахований на курс")
    void usePromoCode_alreadyEnrolled_throws() {
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);

        PromoCodeEntity entity = promoCodeEntity("DOUBLE25");
        when(promoCodeRepository.findByCodeIgnoreCase("DOUBLE25")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), regularUser.getId()))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), courseId))
                .thenReturn(Optional.of(new EnrollmentEntity()));

        assertThatThrownBy(() -> promoCodeService.usePromoCode("DOUBLE25", courseId, "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already enrolled");

        verify(enrollmentService, never()).createEnrollment(any());
        verify(promoCodeUsageRepository, never()).save(any());
    }

    @Test
    @DisplayName("usePromoCode — кидає якщо немає discount для курсу")
    void usePromoCode_noDiscountForCourse_throws() {
        UUID courseId = UUID.randomUUID();
        UUID otherCourseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);

        PromoCodeEntity entity = promoCodeEntity("SPECIFIC");
        CourseEntity otherCourse = new CourseEntity();
        otherCourse.setId(otherCourseId);
        PromoCodeDiscountEntity discount = new PromoCodeDiscountEntity();
        discount.setDiscountType(DiscountType.FIXED_AMOUNT);
        discount.setDiscountValue(BigDecimal.TEN);
        discount.setCourse(otherCourse);
        entity.getDiscounts().add(discount);

        when(promoCodeRepository.findByCodeIgnoreCase("SPECIFIC")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), regularUser.getId()))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), courseId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoCodeService.usePromoCode("SPECIFIC", courseId, "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No discount found");

        verify(enrollmentService, never()).createEnrollment(any());
    }

    @Test
    @DisplayName("usePromoCode — FIXED_PRICE discount зберігає правильну finalPrice")
    void usePromoCode_fixedPriceDiscount_correctFinalPrice() {
        UUID courseId = UUID.randomUUID();
        CourseEntity course = new CourseEntity();
        course.setId(courseId);
        course.setPrice(new BigDecimal("200.00"));

        PromoCodeEntity entity = promoCodeEntity("FLAT99");
        PromoCodeDiscountEntity discount = new PromoCodeDiscountEntity();
        discount.setDiscountType(DiscountType.FIXED_PRICE);
        discount.setDiscountValue(new BigDecimal("99.00"));
        discount.setCourse(null);
        entity.getDiscounts().add(discount);

        when(promoCodeRepository.findByCodeIgnoreCase("FLAT99")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(regularUser));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), regularUser.getId()))
                .thenReturn(false);
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(enrollmentRepository.findByStudentIdAndCourseId(regularUser.getId(), courseId))
                .thenReturn(Optional.empty());

        promoCodeService.usePromoCode("FLAT99", courseId, "user@test.com");

        ArgumentCaptor<PromoCodeUsageEntity> captor = ArgumentCaptor.forClass(PromoCodeUsageEntity.class);
        verify(promoCodeUsageRepository).save(captor.capture());
        assertThat(captor.getValue().getFinalPrice()).isEqualByComparingTo("99.00");
    }

    // ─────────────────────── updatePromoCode ───────────────────────

    @Test
    @DisplayName("updatePromoCode — ADMIN успішно оновлює будь-який промокод")
    void updatePromoCode_admin_success() {
        PromoCodeEntity entity = promoCodeEntity("OLD");
        entity.setCreatedBy(fakeAdmin);

        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(promoCodeRepository.findByCodeIgnoreCase("UPDATED")).thenReturn(Optional.empty());
        when(promoCodeRepository.save(entity)).thenReturn(entity);

        PromoCodeCreateDto dto = buildDto("UPDATED", PromoCodeScope.GLOBAL, null);
        promoCodeService.updatePromoCode(entity.getId(), dto, "admin@test.com");

        verify(promoCodeRepository).save(entity);
        assertThat(entity.getCode()).isEqualTo("UPDATED");
    }

    @Test
    @DisplayName("updatePromoCode — FAKE_ADMIN кидає якщо оновлює чужий промокод")
    void updatePromoCode_fakeAdmin_othersCode_throws() {
        PromoCodeEntity entity = promoCodeEntity("FOREIGN");
        entity.setCreatedBy(admin); // створено адміном, не fakeAdmin

        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));
        when(promoCodeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        PromoCodeCreateDto dto = buildDto("FOREIGN", PromoCodeScope.GLOBAL, null);

        assertThatThrownBy(() -> promoCodeService.updatePromoCode(entity.getId(), dto, "fake@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own promo codes");

        verify(promoCodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("updatePromoCode — кидає якщо промокод не знайдено")
    void updatePromoCode_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoCodeService.updatePromoCode(id,
                buildDto("NEW", PromoCodeScope.GLOBAL, null), "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Promo code not found");

        verify(promoCodeRepository, never()).save(any());
    }

    // ─────────────────────── deletePromoCode ───────────────────────

    @Test
    @DisplayName("deletePromoCode — ADMIN успішно видаляє будь-який промокод")
    void deletePromoCode_admin_success() {
        PromoCodeEntity entity = promoCodeEntity("DEL25");
        entity.setCreatedBy(fakeAdmin);

        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        promoCodeService.deletePromoCode(entity.getId(), "admin@test.com");

        verify(promoCodeRepository).delete(entity);
    }

    @Test
    @DisplayName("deletePromoCode — FAKE_ADMIN успішно видаляє СВІЙ промокод")
    void deletePromoCode_fakeAdmin_ownCode_success() {
        PromoCodeEntity entity = promoCodeEntity("OWN50");
        entity.setCreatedBy(fakeAdmin);

        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));
        when(promoCodeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        promoCodeService.deletePromoCode(entity.getId(), "fake@test.com");

        verify(promoCodeRepository).delete(entity);
    }

    @Test
    @DisplayName("deletePromoCode — FAKE_ADMIN кидає IllegalArgumentException для чужого промокоду")
    void deletePromoCode_fakeAdmin_othersCode_throws() {
        PromoCodeEntity entity = promoCodeEntity("FOREIGN25");
        entity.setCreatedBy(admin); // не fakeAdmin

        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));
        when(promoCodeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> promoCodeService.deletePromoCode(entity.getId(), "fake@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own promo codes");

        verify(promoCodeRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deletePromoCode — кидає IllegalArgumentException якщо промокод не знайдено")
    void deletePromoCode_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoCodeService.deletePromoCode(id, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Promo code not found");

        verify(promoCodeRepository, never()).delete(any());
    }

    // ─────────────────────── activateScheduledPromoCodes ───────────────────────

    @Test
    @DisplayName("activateScheduledPromoCodes — активує INACTIVE промокоди з validFrom що настав")
    void activateScheduledPromoCodes_activatesDueCode() {
        PromoCodeEntity due = promoCodeEntity("SCHED50");
        due.setStatus(PromoCodeStatus.INACTIVE);
        due.setValidFrom(LocalDateTime.now().minusMinutes(1));

        when(promoCodeRepository.findByStatusAndValidFromLessThanEqual(
                eq(PromoCodeStatus.INACTIVE), any(LocalDateTime.class)))
                .thenReturn(List.of(due));

        promoCodeService.activateScheduledPromoCodes();

        assertThat(due.getStatus()).isEqualTo(PromoCodeStatus.ACTIVE);
        assertThat(due.getStatusUpdatedAt()).isNotNull();
        verify(promoCodeRepository).saveAll(List.of(due));
    }

    @Test
    @DisplayName("activateScheduledPromoCodes — нічого не робить якщо немає промокодів для активації")
    void activateScheduledPromoCodes_noCodesFound_doesNotSave() {
        when(promoCodeRepository.findByStatusAndValidFromLessThanEqual(any(), any()))
                .thenReturn(List.of());

        promoCodeService.activateScheduledPromoCodes();

        verify(promoCodeRepository, never()).saveAll(any());
    }

    // ─────────────────────── helpers ───────────────────────

    private PromoCodeCreateDto buildDto(String code, PromoCodeScope scope, LocalDateTime validUntil) {
        PromoCodeCreateDto dto = new PromoCodeCreateDto();
        dto.setCode(code);
        dto.setScope(scope);
        dto.setStatus(PromoCodeStatus.ACTIVE);
        dto.setValidUntil(validUntil);
        return dto;
    }

    private PromoCodeEntity promoCodeEntity(String code) {
        PromoCodeEntity e = new PromoCodeEntity();
        e.setId(UUID.randomUUID());
        e.setCode(code.toUpperCase());
        e.setStatus(PromoCodeStatus.ACTIVE);
        e.setScope(PromoCodeScope.GLOBAL);
        e.setTargetPersons(new HashSet<>());
        e.setDiscounts(new ArrayList<>());
        e.setUsages(new ArrayList<>());
        return e;
    }
}
