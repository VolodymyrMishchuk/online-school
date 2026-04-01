package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class EnrollmentRepositoryTest extends AbstractRepositoryTest {

    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private PersonRepository     personRepository;
    @Autowired private CourseRepository     courseRepository;

    private PersonEntity alice;
    private PersonEntity bob;
    private CourseEntity courseA;
    private CourseEntity courseB;

    @BeforeEach
    void setUp() {
        alice   = personRepository.save(person("alice@test.com"));
        bob     = personRepository.save(person("bob@test.com"));
        courseA = courseRepository.save(course("Java Basics"));
        courseB = courseRepository.save(course("Spring Boot"));
    }

    // ─────────────────────── findByStudentId ───────────────────────

    @Test
    @DisplayName("findByStudentId — повертає всі зарахування студента")
    void findByStudentId_returnsAllForStudent() {
        enrollmentRepository.save(enrollment(alice, courseA, "ACTIVE"));
        enrollmentRepository.save(enrollment(alice, courseB, "ACTIVE"));
        enrollmentRepository.save(enrollment(bob,   courseA, "ACTIVE"));

        List<EnrollmentEntity> result = enrollmentRepository.findByStudentId(alice.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(e -> e.getStudent().getId())
                .containsOnly(alice.getId());
    }

    @Test
    @DisplayName("findByStudentId — повертає порожній список якщо немає зарахувань")
    void findByStudentId_noEnrollments_returnsEmpty() {
        List<EnrollmentEntity> result = enrollmentRepository.findByStudentId(alice.getId());
        assertThat(result).isEmpty();
    }

    // ─────────────────────── findByStudentIdAndCourseId ───────────────────────

    @Test
    @DisplayName("findByStudentIdAndCourseId — знаходить зарахування за студентом і курсом")
    void findByStudentIdAndCourseId_found() {
        enrollmentRepository.save(enrollment(alice, courseA, "ACTIVE"));

        Optional<EnrollmentEntity> result = enrollmentRepository
                .findByStudentIdAndCourseId(alice.getId(), courseA.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getStudent().getId()).isEqualTo(alice.getId());
        assertThat(result.get().getCourse().getId()).isEqualTo(courseA.getId());
    }

    @Test
    @DisplayName("findByStudentIdAndCourseId — повертає empty якщо зарахування немає")
    void findByStudentIdAndCourseId_notFound_returnsEmpty() {
        Optional<EnrollmentEntity> result = enrollmentRepository
                .findByStudentIdAndCourseId(alice.getId(), courseA.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByStudentIdAndCourseId — bob на courseA не знаходить alice на courseA")
    void findByStudentIdAndCourseId_wrongStudent_returnsEmpty() {
        enrollmentRepository.save(enrollment(bob, courseA, "ACTIVE"));

        Optional<EnrollmentEntity> result = enrollmentRepository
                .findByStudentIdAndCourseId(alice.getId(), courseA.getId());

        assertThat(result).isEmpty();
    }

    // ─────────────────────── findByStatus ───────────────────────

    @Test
    @DisplayName("findByStatus — повертає тільки зарахування з вказаним статусом")
    void findByStatus_returnsOnlyMatchingStatus() {
        enrollmentRepository.save(enrollment(alice, courseA, "ACTIVE"));
        enrollmentRepository.save(enrollment(bob,   courseA, "EXPIRED"));

        List<EnrollmentEntity> active = enrollmentRepository.findByStatus("ACTIVE");

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getStatus()).isEqualTo("ACTIVE");
    }

    // ─────────────────────── findByStatusAndExpiresAtBetween ───────────────────────

    @Test
    @DisplayName("findByStatusAndExpiresAtBetween — повертає зарахування з expiresAt у діапазоні")
    void findByStatusAndExpiresAtBetween_returnsInRange() {
        OffsetDateTime now = OffsetDateTime.now();

        EnrollmentEntity expiresIn3Days = enrollment(alice, courseA, "ACTIVE");
        expiresIn3Days.setExpiresAt(now.plusDays(3));
        enrollmentRepository.save(expiresIn3Days);

        EnrollmentEntity expiresIn10Days = enrollment(bob, courseA, "ACTIVE");
        expiresIn10Days.setExpiresAt(now.plusDays(10));
        enrollmentRepository.save(expiresIn10Days);

        // Шукаємо ті що закінчуються за 1-7 днів
        List<EnrollmentEntity> result = enrollmentRepository
                .findByStatusAndExpiresAtBetween("ACTIVE", now.plusDays(1), now.plusDays(7));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStudent().getId()).isEqualTo(alice.getId());
    }

    @Test
    @DisplayName("findByStatusAndExpiresAtBetween — не повертає зарахування поза діапазоном")
    void findByStatusAndExpiresAtBetween_outsideRange_returnsEmpty() {
        OffsetDateTime now = OffsetDateTime.now();
        EnrollmentEntity e = enrollment(alice, courseA, "ACTIVE");
        e.setExpiresAt(now.plusDays(30));
        enrollmentRepository.save(e);

        List<EnrollmentEntity> result = enrollmentRepository
                .findByStatusAndExpiresAtBetween("ACTIVE", now.plusDays(1), now.plusDays(7));

        assertThat(result).isEmpty();
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        return p;
    }

    private CourseEntity course(String name) {
        CourseEntity c = new CourseEntity();
        c.setName(name);
        return c;
    }

    private EnrollmentEntity enrollment(PersonEntity student, CourseEntity course, String status) {
        EnrollmentEntity e = new EnrollmentEntity();
        e.setStudent(student);
        e.setCourse(course);
        e.setStatus(status);
        return e;
    }
}
