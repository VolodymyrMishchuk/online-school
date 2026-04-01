package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import com.mishchuk.onlineschool.repository.entity.PromoCodeUsageEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PromoCodeUsageRepositoryTest extends AbstractRepositoryTest {

    @Autowired private PromoCodeUsageRepository promoCodeUsageRepository;
    @Autowired private PromoCodeRepository      promoCodeRepository;
    @Autowired private PersonRepository         personRepository;
    @Autowired private CourseRepository         courseRepository;

    private PersonEntity alice;
    private PersonEntity bob;
    private PromoCodeEntity promoCode;
    private CourseEntity    course;

    @BeforeEach
    void setUp() {
        alice     = personRepository.save(person("alice@test.com"));
        bob       = personRepository.save(person("bob@test.com"));
        promoCode = promoCodeRepository.save(promoCode("SAVE20"));
        course    = courseRepository.save(course("Java 101"));
    }

    // ─────────────────────── findByPromoCodeIdAndPersonId ───────────────────────

    @Test
    @DisplayName("findByPromoCodeIdAndPersonId — знаходить usage за промокодом і персоною")
    void findByPromoCodeIdAndPersonId_found() {
        promoCodeUsageRepository.save(usage(promoCode, alice));

        Optional<PromoCodeUsageEntity> result = promoCodeUsageRepository
                .findByPromoCodeIdAndPersonId(promoCode.getId(), alice.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getPerson().getId()).isEqualTo(alice.getId());
        assertThat(result.get().getPromoCode().getId()).isEqualTo(promoCode.getId());
    }

    @Test
    @DisplayName("findByPromoCodeIdAndPersonId — повертає empty якщо usage не існує")
    void findByPromoCodeIdAndPersonId_notFound_returnsEmpty() {
        Optional<PromoCodeUsageEntity> result = promoCodeUsageRepository
                .findByPromoCodeIdAndPersonId(promoCode.getId(), UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPromoCodeIdAndPersonId — не повертає usage іншої персони")
    void findByPromoCodeIdAndPersonId_wrongPerson_returnsEmpty() {
        promoCodeUsageRepository.save(usage(promoCode, alice));

        Optional<PromoCodeUsageEntity> result = promoCodeUsageRepository
                .findByPromoCodeIdAndPersonId(promoCode.getId(), bob.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByPromoCodeIdAndPersonId — не повертає usage іншого промокоду")
    void findByPromoCodeIdAndPersonId_wrongPromoCode_returnsEmpty() {
        promoCodeUsageRepository.save(usage(promoCode, alice));

        Optional<PromoCodeUsageEntity> result = promoCodeUsageRepository
                .findByPromoCodeIdAndPersonId(UUID.randomUUID(), alice.getId());

        assertThat(result).isEmpty();
    }

    // ─────────────────────── existsByPromoCodeIdAndPersonId ───────────────────────

    @Test
    @DisplayName("existsByPromoCodeIdAndPersonId — повертає true якщо usage існує")
    void existsByPromoCodeIdAndPersonId_exists_returnsTrue() {
        promoCodeUsageRepository.save(usage(promoCode, alice));

        boolean result = promoCodeUsageRepository
                .existsByPromoCodeIdAndPersonId(promoCode.getId(), alice.getId());

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("existsByPromoCodeIdAndPersonId — повертає false якщо usage не існує")
    void existsByPromoCodeIdAndPersonId_notExists_returnsFalse() {
        boolean result = promoCodeUsageRepository
                .existsByPromoCodeIdAndPersonId(promoCode.getId(), alice.getId());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("existsByPromoCodeIdAndPersonId — alice used ≠ bob used (ізоляція між персонами)")
    void existsByPromoCodeIdAndPersonId_aliceUsed_bobNotUsed() {
        promoCodeUsageRepository.save(usage(promoCode, alice));

        assertThat(promoCodeUsageRepository
                .existsByPromoCodeIdAndPersonId(promoCode.getId(), alice.getId())).isTrue();
        assertThat(promoCodeUsageRepository
                .existsByPromoCodeIdAndPersonId(promoCode.getId(), bob.getId())).isFalse();
    }

    @Test
    @DisplayName("existsByPromoCodeIdAndPersonId — той самий person, різні промокоди незалежні")
    void existsByPromoCodeIdAndPersonId_twoPromoCodes_independent() {
        PromoCodeEntity other = promoCodeRepository.save(promoCode("OTHER10"));
        promoCodeUsageRepository.save(usage(promoCode, alice));

        assertThat(promoCodeUsageRepository
                .existsByPromoCodeIdAndPersonId(promoCode.getId(), alice.getId())).isTrue();
        assertThat(promoCodeUsageRepository
                .existsByPromoCodeIdAndPersonId(other.getId(), alice.getId())).isFalse();
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        return p;
    }

    private PromoCodeEntity promoCode(String code) {
        PromoCodeEntity pc = new PromoCodeEntity();
        pc.setCode(code);
        pc.setStatus(PromoCodeStatus.ACTIVE);
        pc.setScope(PromoCodeScope.GLOBAL);
        return pc;
    }

    private CourseEntity course(String name) {
        CourseEntity c = new CourseEntity();
        c.setName(name);
        return c;
    }

    private PromoCodeUsageEntity usage(PromoCodeEntity promoCode, PersonEntity person) {
        PromoCodeUsageEntity u = new PromoCodeUsageEntity();
        u.setPromoCode(promoCode);
        u.setPerson(person);
        u.setCourse(course);
        return u;
    }
}
