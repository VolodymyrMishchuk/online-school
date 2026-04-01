package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;
import com.mishchuk.onlineschool.repository.entity.PromoCodeScope;
import com.mishchuk.onlineschool.repository.entity.PromoCodeStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;



import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PromoCodeRepositoryImpl.findPaginatedPromoCodes —
 * a native SQL query with dynamic WHERE and ORDER BY.
 *
 * We verify:
 *  — pagination (page size, total count)
 *  — search by code (ILIKE)
 *  — search by target person name/email
 *  — creatorId filter
 *  — statusSort (ACTIVE to top/bottom)
 *  — sortKey (code, status, createdAt)
 */
class PromoCodeRepositoryImplTest extends AbstractRepositoryTest {

    @Autowired private PromoCodeRepository promoCodeRepository;
    @Autowired private PersonRepository    personRepository;

    private PersonEntity creator;
    private PersonEntity targetUser;

    private PromoCodeEntity activeCode;

    @BeforeEach
    void setUp() {
        creator    = personRepository.save(person("creator@test.com", "Creator", "Admin"));
        targetUser = personRepository.save(person("target@test.com", "Target", "User"));

        activeCode   = promoCodeRepository.save(promoCode("SUMMER10", PromoCodeStatus.ACTIVE, creator));
        // inactiveCode — збережений в БД, використовується у statusSort/pagination тестах через запит
        promoCodeRepository.save(promoCode("WINTER20", PromoCodeStatus.INACTIVE, creator));
    }

    // ─────────────────────── pagination ───────────────────────

    @Test
    @DisplayName("findPaginatedPromoCodes — повертає всі коди на першій сторінці")
    void pagination_returnsAllOnFirstPage() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, null, "asc", null, PageRequest.of(0, 10), null);

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findPaginatedPromoCodes — пагінація обмежує кількість результатів")
    void pagination_pageSize_limitsResults() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, null, "asc", null, PageRequest.of(0, 1), null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    // ─────────────────────── search by code ───────────────────────

    @Test
    @DisplayName("findPaginatedPromoCodes — search по code знаходить потрібний код")
    void search_byCode_filtersCorrectly() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                "SUMMER", null, "asc", null, PageRequest.of(0, 10), null);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCode()).isEqualTo("SUMMER10");
    }

    @Test
    @DisplayName("findPaginatedPromoCodes — search без збігів повертає порожній результат")
    void search_noMatch_returnsEmpty() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                "ZZZNOMATCH", null, "asc", null, PageRequest.of(0, 10), null);

        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findPaginatedPromoCodes — null search повертає всі коди")
    void search_null_returnsAll() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, null, "asc", null, PageRequest.of(0, 10), null);

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    // ─────────────────────── search by target person ───────────────────────

    @Test
    @DisplayName("findPaginatedPromoCodes — search по імені target person знаходить відповідний код")
    void search_byTargetPersonName_filtersCorrectly() {
        // Прив'язуємо targetUser до activeCode
        activeCode.setTargetPersons(new java.util.HashSet<>(java.util.Set.of(targetUser)));
        promoCodeRepository.save(activeCode);

        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                "Target", null, "asc", null, PageRequest.of(0, 10), null);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getCode()).isEqualTo("SUMMER10");
    }

    // ─────────────────────── creatorId filter ───────────────────────

    @Test
    @DisplayName("findPaginatedPromoCodes — creatorId фільтрує за творцем")
    void creatorIdFilter_returnsOnlyCreatorCodes() {
        PersonEntity other = personRepository.save(person("other@test.com", "Other", "Person"));
        promoCodeRepository.save(promoCode("OTHER30", PromoCodeStatus.ACTIVE, other));

        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, null, "asc", null, PageRequest.of(0, 10), creator.getId());

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(PromoCodeEntity::getCode)
                .containsExactlyInAnyOrder("SUMMER10", "WINTER20");
    }

    @Test
    @DisplayName("findPaginatedPromoCodes — null creatorId повертає всі коди")
    void creatorIdFilter_null_returnsAll() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, null, "asc", null, PageRequest.of(0, 10), null);

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    // ─────────────────────── statusSort ───────────────────────

    @Test
    @DisplayName("findPaginatedPromoCodes — statusSort=top ставить ACTIVE першим")
    void statusSort_top_putsActiveFirst() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, null, "asc", "top", PageRequest.of(0, 10), null);

        assertThat(page.getContent().get(0).getStatus()).isEqualTo(PromoCodeStatus.ACTIVE);
    }

    @Test
    @DisplayName("findPaginatedPromoCodes — statusSort=bottom ставить ACTIVE останнім")
    void statusSort_bottom_putsActiveFirst() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, null, "asc", "bottom", PageRequest.of(0, 10), null);

        int last = page.getContent().size() - 1;
        assertThat(page.getContent().get(last).getStatus()).isEqualTo(PromoCodeStatus.ACTIVE);
    }

    // ─────────────────────── sortKey ───────────────────────

    @Test
    @DisplayName("findPaginatedPromoCodes — sortKey=code ASC сортує за кодом")
    void sortKey_code_asc() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, "code", "asc", null, PageRequest.of(0, 10), null);

        assertThat(page.getContent())
                .extracting(PromoCodeEntity::getCode)
                .isSorted();
    }

    @Test
    @DisplayName("findPaginatedPromoCodes — sortKey=code DESC сортує у зворотному порядку")
    void sortKey_code_desc() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, "code", "desc", null, PageRequest.of(0, 10), null);

        assertThat(page.getContent())
                .extracting(PromoCodeEntity::getCode)
                .isSortedAccordingTo((a, b) -> b.compareTo(a));
    }

    @Test
    @DisplayName("findPaginatedPromoCodes — sortKey=status сортує за статусом")
    void sortKey_status_asc() {
        Page<PromoCodeEntity> page = promoCodeRepository.findPaginatedPromoCodes(
                null, "status", "asc", null, PageRequest.of(0, 10), null);

        // ACTIVE < INACTIVE за алфавітом
        assertThat(page.getContent().get(0).getStatus()).isEqualTo(PromoCodeStatus.ACTIVE);
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email, String firstName, String lastName) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        p.setFirstName(firstName);
        p.setLastName(lastName);
        return p;
    }

    private PromoCodeEntity promoCode(String code, PromoCodeStatus status, PersonEntity creator) {
        PromoCodeEntity pc = new PromoCodeEntity();
        pc.setCode(code);
        pc.setStatus(status);
        pc.setScope(PromoCodeScope.GLOBAL);
        pc.setCreatedBy(creator);
        return pc;
    }
}
