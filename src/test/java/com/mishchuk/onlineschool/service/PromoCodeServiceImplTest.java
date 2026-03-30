package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.dto.PromoCodeCreateDto;
import com.mishchuk.onlineschool.repository.CourseRepository;
import com.mishchuk.onlineschool.repository.EnrollmentRepository;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.PromoCodeRepository;
import com.mishchuk.onlineschool.repository.PromoCodeUsageRepository;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
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

    // --- createPromoCode ---

    @Test
    @DisplayName("createPromoCode — кидає IllegalArgumentException якщо код вже існує")
    void createPromoCode_duplicateCode_throws() {
        PersonEntity admin = admin();
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        PromoCodeCreateDto dto = promoCodeCreateDto("SPRING25");
        when(promoCodeRepository.findByCodeIgnoreCase("SPRING25")).thenReturn(Optional.of(new PromoCodeEntity()));

        assertThatThrownBy(() -> promoCodeService.createPromoCode(dto, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("createPromoCode — зберігає новий промокод")
    void createPromoCode_success_savesEntity() {
        PersonEntity admin = admin();
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        PromoCodeCreateDto dto = promoCodeCreateDto("SUMMER25");
        when(promoCodeRepository.findByCodeIgnoreCase("SUMMER25")).thenReturn(Optional.empty());

        PromoCodeEntity saved = promoCodeEntity("SUMMER25");
        when(promoCodeRepository.save(any(PromoCodeEntity.class))).thenReturn(saved);

        promoCodeService.createPromoCode(dto, "admin@test.com");

        verify(promoCodeRepository).save(any(PromoCodeEntity.class));
    }

    @Test
    @DisplayName("createPromoCode — кидає помилку якщо PERSONAL без targetPersonIds")
    void createPromoCode_personalNoTargets_throws() {
        PersonEntity admin = admin();
        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));

        PromoCodeCreateDto dto = promoCodeCreateDto("PERS25");
        dto.setScope(PromoCodeScope.PERSONAL);
        when(promoCodeRepository.findByCodeIgnoreCase("PERS25")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> promoCodeService.createPromoCode(dto, "admin@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target persons");
    }

    // --- checkPromoCode ---

    @Test
    @DisplayName("checkPromoCode — кидає якщо промокод неактивний")
    void checkPromoCode_inactive_throws() {
        PromoCodeEntity entity = promoCodeEntity("OFF50");
        entity.setStatus(PromoCodeStatus.INACTIVE);

        when(promoCodeRepository.findByCodeIgnoreCase("OFF50")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("OFF50", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not active");
    }

    @Test
    @DisplayName("checkPromoCode — кидає якщо промокод вже використовувався")
    void checkPromoCode_alreadyUsed_throws() {
        PersonEntity user = new PersonEntity();
        user.setId(UUID.randomUUID());
        PromoCodeEntity entity = promoCodeEntity("USED10");

        when(promoCodeRepository.findByCodeIgnoreCase("USED10")).thenReturn(Optional.of(entity));
        when(personRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(promoCodeUsageRepository.existsByPromoCodeIdAndPersonId(entity.getId(), user.getId())).thenReturn(true);

        assertThatThrownBy(() -> promoCodeService.checkPromoCode("USED10", "user@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already used");
    }

    // --- deletePromoCode ---

    @Test
    @DisplayName("deletePromoCode — видаляє промокод який знайдено")
    void deletePromoCode_success() {
        PersonEntity admin = admin();
        PromoCodeEntity entity = promoCodeEntity("DEL25");
        entity.setCreatedBy(admin);

        when(personRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(promoCodeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        promoCodeService.deletePromoCode(entity.getId(), "admin@test.com");

        verify(promoCodeRepository).delete(entity);
    }

    @Test
    @DisplayName("deletePromoCode — FAKE_ADMIN не може видалити чужий промокод")
    void deletePromoCode_fakeAdmin_othersCode_throws() {
        PersonEntity fakeAdmin = new PersonEntity();
        fakeAdmin.setId(UUID.randomUUID());
        fakeAdmin.setRole(PersonRole.FAKE_ADMIN);

        PersonEntity owner = new PersonEntity();
        owner.setId(UUID.randomUUID());

        PromoCodeEntity entity = promoCodeEntity("FOREIGN25");
        entity.setCreatedBy(owner);

        when(personRepository.findByEmail("fake@test.com")).thenReturn(Optional.of(fakeAdmin));
        when(promoCodeRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> promoCodeService.deletePromoCode(entity.getId(), "fake@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own promo codes");
    }

    // helpers

    private PersonEntity admin() {
        PersonEntity a = new PersonEntity();
        a.setId(UUID.randomUUID());
        a.setEmail("admin@test.com");
        a.setRole(PersonRole.ADMIN);
        return a;
    }

    private PromoCodeCreateDto promoCodeCreateDto(String code) {
        PromoCodeCreateDto dto = new PromoCodeCreateDto();
        dto.setCode(code);
        dto.setScope(PromoCodeScope.GLOBAL);
        dto.setStatus(PromoCodeStatus.ACTIVE);
        return dto;
    }

    private PromoCodeEntity promoCodeEntity(String code) {
        PromoCodeEntity e = new PromoCodeEntity();
        e.setId(UUID.randomUUID());
        e.setCode(code.toUpperCase());
        e.setStatus(PromoCodeStatus.ACTIVE);
        e.setScope(PromoCodeScope.GLOBAL);
        e.setTargetPersons(new java.util.HashSet<>());
        e.setDiscounts(new ArrayList<>());
        e.setUsages(new ArrayList<>());
        return e;
    }
}
