package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PersonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for PersonRepositoryImpl.findPaginatedUsers —
 * a native SQL query with dynamic WHERE, ORDER BY, and pagination.
 *
 * We verify:
 *  — basic pagination (page size, total count)
 *  — search filter (ILIKE on firstName, lastName, email)
 *  — blockedSort (BLOCKED users to top/bottom)
 *  — adminSort (ADMIN users to top/bottom)
 *  — sortKey (name, role, status, createdAt)
 *  — null/empty search → returns all
 */
class PersonRepositoryImplTest extends AbstractRepositoryTest {

    @Autowired private PersonRepository     personRepository;
    @Autowired private CourseRepository     courseRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;

    private PersonEntity alice;
    private PersonEntity bob;

    @BeforeEach
    void setUp() {
        alice = personRepository.save(person("alice@test.com", "Alice", "Smith",
                PersonRole.USER, PersonStatus.ACTIVE));
        bob   = personRepository.save(person("bob@test.com", "Bob", "Jones",
                PersonRole.ADMIN, PersonStatus.ACTIVE));
        // carol (BLOCKED) — збережена в БД, використовується у blockedSort тестах через запит
        personRepository.save(person("carol@test.com", "Carol", "Brown",
                PersonRole.USER, PersonStatus.BLOCKED));
    }

    // ─────────────────────── pagination ───────────────────────

    @Test
    @DisplayName("findPaginatedUsers — повертає коректну кількість елементів на сторінці")
    void pagination_pageSize_limitsResults() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, null, "asc", null, null, PageRequest.of(0, 2));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("findPaginatedUsers — друга сторінка містить залишок")
    void pagination_secondPage_returnsRemainder() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, null, "asc", null, null, PageRequest.of(1, 2));

        assertThat(page.getContent()).hasSize(1);
    }

    // ─────────────────────── search ───────────────────────

    @Test
    @DisplayName("findPaginatedUsers — search по firstName знаходить відповідні записи")
    void search_byFirstName_filtersCorrectly() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                "alice", null, "asc", null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("findPaginatedUsers — search по email знаходить відповідні записи")
    void search_byEmail_filtersCorrectly() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                "bob@test", null, "asc", null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("bob@test.com");
    }

    @Test
    @DisplayName("findPaginatedUsers — search без збігів повертає порожній результат")
    void search_noMatch_returnsEmpty() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                "zzznomatch", null, "asc", null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findPaginatedUsers — null search повертає всіх користувачів")
    void search_null_returnsAll() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, null, "asc", null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("findPaginatedUsers — порожній search повертає всіх користувачів")
    void search_empty_returnsAll() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                "  ", null, "asc", null, null, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    // ─────────────────────── blockedSort ───────────────────────

    @Test
    @DisplayName("findPaginatedUsers — blockedSort=top ставить BLOCKED першим")
    void blockedSort_top_putsBlockedFirst() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, null, "asc", "top", null, PageRequest.of(0, 10));

        assertThat(page.getContent().get(0).getStatus()).isEqualTo(PersonStatus.BLOCKED);
    }

    @Test
    @DisplayName("findPaginatedUsers — blockedSort=bottom ставить BLOCKED останнім")
    void blockedSort_bottom_putsBlockedLast() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, null, "asc", "bottom", null, PageRequest.of(0, 10));

        int last = page.getContent().size() - 1;
        assertThat(page.getContent().get(last).getStatus()).isEqualTo(PersonStatus.BLOCKED);
    }

    // ─────────────────────── adminSort ───────────────────────

    @Test
    @DisplayName("findPaginatedUsers — adminSort=top ставить ADMIN першим")
    void adminSort_top_putsAdminFirst() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, null, "asc", null, "top", PageRequest.of(0, 10));

        assertThat(page.getContent().get(0).getRole()).isEqualTo(PersonRole.ADMIN);
    }

    @Test
    @DisplayName("findPaginatedUsers — adminSort=bottom ставить ADMIN останнім")
    void adminSort_bottom_putsAdminLast() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, null, "asc", null, "bottom", PageRequest.of(0, 10));

        int last = page.getContent().size() - 1;
        assertThat(page.getContent().get(last).getRole()).isEqualTo(PersonRole.ADMIN);
    }

    // ─────────────────────── sortKey ───────────────────────

    @Test
    @DisplayName("findPaginatedUsers — sortKey=name ASC сортує за ім'ям")
    void sortKey_name_asc() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, "name", "asc", null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(PersonEntity::getFirstName)
                .isSortedAccordingTo(String.CASE_INSENSITIVE_ORDER);
    }

    @Test
    @DisplayName("findPaginatedUsers — sortKey=name DESC сортує у зворотному порядку")
    void sortKey_name_desc() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, "name", "desc", null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .extracting(PersonEntity::getFirstName)
                .isSortedAccordingTo((a, b) -> b.compareToIgnoreCase(a));
    }

    @Test
    @DisplayName("findPaginatedUsers — sortKey=role сортує за роллю")
    void sortKey_role_asc() {
        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, "role", "asc", null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).isNotEmpty();
        // ADMIN < USER за алфавітом
        assertThat(page.getContent().get(0).getRole()).isEqualTo(PersonRole.ADMIN);
    }

    @Test
    @DisplayName("findPaginatedUsers — sortKey=enrollments сортує за кількістю зарахувань")
    void sortKey_enrollments_desc() {
        // bob записаний на 2 курси, alice на 1
        CourseEntity c1 = courseRepository.save(course("C1"));
        CourseEntity c2 = courseRepository.save(course("C2"));
        enrollmentRepository.save(enrollment(alice, c1));
        enrollmentRepository.save(enrollment(bob, c1));
        enrollmentRepository.save(enrollment(bob, c2));

        Page<PersonEntity> page = personRepository.findPaginatedUsers(
                null, "enrollments", "desc", null, null, PageRequest.of(0, 10));

        // Перший має бути bob (2 enrollments)
        assertThat(page.getContent().get(0).getEmail()).isEqualTo("bob@test.com");
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email, String firstName, String lastName,
                                PersonRole role, PersonStatus status) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        p.setFirstName(firstName);
        p.setLastName(lastName);
        p.setRole(role);
        p.setStatus(status);
        return p;
    }

    private CourseEntity course(String name) {
        CourseEntity c = new CourseEntity();
        c.setName(name);
        return c;
    }

    private EnrollmentEntity enrollment(PersonEntity student, CourseEntity course) {
        EnrollmentEntity e = new EnrollmentEntity();
        e.setStudent(student);
        e.setCourse(course);
        e.setStatus("ACTIVE");
        return e;
    }
}
