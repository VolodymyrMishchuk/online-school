package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PromoCodeRepositoryTest extends AbstractRepositoryTest {

    @Autowired private PromoCodeRepository promoCodeRepository;
    @Autowired private PersonRepository    personRepository;

    private PersonEntity creator;

    @BeforeEach
    void setUp() {
        creator = personRepository.save(person("admin@test.com"));
    }

    // ─────────────────────── findByCodeIgnoreCase ───────────────────────

    @Test
    @DisplayName("findByCodeIgnoreCase — знаходить код в точному регістрі")
    void findByCodeIgnoreCase_exactCase_found() {
        promoCodeRepository.save(promoCode("SUMMER10", PromoCodeStatus.ACTIVE));

        Optional<PromoCodeEntity> result = promoCodeRepository.findByCodeIgnoreCase("SUMMER10");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("SUMMER10");
    }

    @Test
    @DisplayName("findByCodeIgnoreCase — знаходить код у нижньому регістрі")
    void findByCodeIgnoreCase_lowercase_found() {
        promoCodeRepository.save(promoCode("SUMMER10", PromoCodeStatus.ACTIVE));

        Optional<PromoCodeEntity> result = promoCodeRepository.findByCodeIgnoreCase("summer10");

        assertThat(result).isPresent();
        assertThat(result.get().getCode()).isEqualTo("SUMMER10");
    }

    @Test
    @DisplayName("findByCodeIgnoreCase — знаходить код у мішаному регістрі")
    void findByCodeIgnoreCase_mixedCase_found() {
        promoCodeRepository.save(promoCode("WINTER20", PromoCodeStatus.ACTIVE));

        Optional<PromoCodeEntity> result = promoCodeRepository.findByCodeIgnoreCase("WiNtEr20");

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("findByCodeIgnoreCase — повертає empty якщо код не існує")
    void findByCodeIgnoreCase_notFound_returnsEmpty() {
        Optional<PromoCodeEntity> result = promoCodeRepository.findByCodeIgnoreCase("NOSUCHCODE");

        assertThat(result).isEmpty();
    }

    // ─────────────────────── findByStatusAndValidFromLessThanEqual ───────────────────────

    @Test
    @DisplayName("findByStatusAndValidFromLessThanEqual — знаходить активні коди де validFrom <= заданої дати")
    void findByStatusAndValidFrom_returnsMatchingCodes() {
        LocalDateTime yesterday  = LocalDateTime.now().minusDays(1);
        LocalDateTime tomorrow   = LocalDateTime.now().plusDays(1);

        PromoCodeEntity started  = promoCode("STARTED", PromoCodeStatus.ACTIVE);
        started.setValidFrom(yesterday);
        promoCodeRepository.save(started);

        PromoCodeEntity notYet   = promoCode("NOTYET", PromoCodeStatus.ACTIVE);
        notYet.setValidFrom(tomorrow);
        promoCodeRepository.save(notYet);

        PromoCodeEntity inactive = promoCode("INACTIVE", PromoCodeStatus.INACTIVE);
        inactive.setValidFrom(yesterday);
        promoCodeRepository.save(inactive);

        List<PromoCodeEntity> result = promoCodeRepository
                .findByStatusAndValidFromLessThanEqual(PromoCodeStatus.ACTIVE, LocalDateTime.now());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("STARTED");
    }

    @Test
    @DisplayName("findByStatusAndValidFromLessThanEqual — повертає порожній список якщо немає відповідних кодів")
    void findByStatusAndValidFrom_noMatch_returnsEmpty() {
        PromoCodeEntity future = promoCode("FUTURE", PromoCodeStatus.ACTIVE);
        future.setValidFrom(LocalDateTime.now().plusDays(5));
        promoCodeRepository.save(future);

        List<PromoCodeEntity> result = promoCodeRepository
                .findByStatusAndValidFromLessThanEqual(PromoCodeStatus.ACTIVE, LocalDateTime.now());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByStatusAndValidFromLessThanEqual — validFrom рівно зараз включається (inclusive)")
    void findByStatusAndValidFrom_exactBoundary_included() {
        LocalDateTime boundary = LocalDateTime.now();

        PromoCodeEntity exact = promoCode("EXACT", PromoCodeStatus.ACTIVE);
        exact.setValidFrom(boundary);
        promoCodeRepository.save(exact);

        List<PromoCodeEntity> result = promoCodeRepository
                .findByStatusAndValidFromLessThanEqual(PromoCodeStatus.ACTIVE, boundary);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("findByStatusAndValidFromLessThanEqual — фільтрує по статусу")
    void findByStatusAndValidFrom_statusFilter_works() {
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

        PromoCodeEntity active   = promoCode("ACTIVE10", PromoCodeStatus.ACTIVE);
        active.setValidFrom(yesterday);
        promoCodeRepository.save(active);

        PromoCodeEntity inactive = promoCode("INACTIVE10", PromoCodeStatus.INACTIVE);
        inactive.setValidFrom(yesterday);
        promoCodeRepository.save(inactive);

        List<PromoCodeEntity> activeResult = promoCodeRepository
                .findByStatusAndValidFromLessThanEqual(PromoCodeStatus.ACTIVE, LocalDateTime.now());
        List<PromoCodeEntity> inactiveResult = promoCodeRepository
                .findByStatusAndValidFromLessThanEqual(PromoCodeStatus.INACTIVE, LocalDateTime.now());

        assertThat(activeResult).hasSize(1).extracting(PromoCodeEntity::getCode).containsOnly("ACTIVE10");
        assertThat(inactiveResult).hasSize(1).extracting(PromoCodeEntity::getCode).containsOnly("INACTIVE10");
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        return p;
    }

    private PromoCodeEntity promoCode(String code, PromoCodeStatus status) {
        PromoCodeEntity pc = new PromoCodeEntity();
        pc.setCode(code);
        pc.setStatus(status);
        pc.setScope(PromoCodeScope.GLOBAL);
        pc.setCreatedBy(creator);
        return pc;
    }
}
