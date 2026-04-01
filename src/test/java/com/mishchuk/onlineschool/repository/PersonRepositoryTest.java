package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PersonRepositoryTest extends AbstractRepositoryTest {

    @Autowired private PersonRepository personRepository;

    private PersonEntity admin;
    private PersonEntity alice;
    private PersonEntity bob;
    private PersonEntity creator;

    @BeforeEach
    void setUp() {
        creator = personRepository.save(person("creator@test.com", PersonRole.ADMIN));
        admin   = personRepository.save(person("admin@test.com",   PersonRole.ADMIN));
        alice   = personRepository.save(personCreatedBy("alice@test.com", PersonRole.USER, creator));
        bob     = personRepository.save(personCreatedBy("bob@test.com",   PersonRole.USER, creator));
    }

    // ─────────────────────── findByEmail ───────────────────────

    @Test
    @DisplayName("findByEmail — знаходить користувача за email")
    void findByEmail_found() {
        Optional<PersonEntity> result = personRepository.findByEmail("alice@test.com");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("alice@test.com");
    }

    @Test
    @DisplayName("findByEmail — повертає empty якщо email не існує")
    void findByEmail_notFound_returnsEmpty() {
        Optional<PersonEntity> result = personRepository.findByEmail("nobody@test.com");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByEmail — email чутливий до регістру (exact match)")
    void findByEmail_caseSensitive() {
        // PersonRepository використовує findByEmail (JPA derived query — exact match)
        Optional<PersonEntity> result = personRepository.findByEmail("ALICE@test.com");

        assertThat(result).isEmpty();
    }

    // ─────────────────────── findAllByRole ───────────────────────

    @Test
    @DisplayName("findAllByRole — повертає лише користувачів з вказаною роллю")
    void findAllByRole_returnsOnlyMatchingRole() {
        List<PersonEntity> admins = personRepository.findAllByRole(PersonRole.ADMIN);

        assertThat(admins).hasSize(2); // creator + admin
        assertThat(admins).extracting(PersonEntity::getRole)
                .containsOnly(PersonRole.ADMIN);
    }

    @Test
    @DisplayName("findAllByRole — повертає всіх USER")
    void findAllByRole_users_returnsCorrectCount() {
        List<PersonEntity> users = personRepository.findAllByRole(PersonRole.USER);

        assertThat(users).hasSize(2); // alice + bob
        assertThat(users).extracting(PersonEntity::getEmail)
                .containsExactlyInAnyOrder("alice@test.com", "bob@test.com");
    }

    @Test
    @DisplayName("findAllByRole — повертає порожній список якщо немає з такою роллю")
    void findAllByRole_noMatch_returnsEmpty() {
        List<PersonEntity> fakeAdmins = personRepository.findAllByRole(PersonRole.FAKE_ADMIN);

        assertThat(fakeAdmins).isEmpty();
    }

    // ─────────────────────── findAllByCreatedById ───────────────────────

    @Test
    @DisplayName("findAllByCreatedById — повертає всіх користувачів створених конкретним creator")
    void findAllByCreatedById_returnsCreatedByCreator() {
        List<PersonEntity> created = personRepository.findAllByCreatedById(creator.getId());

        assertThat(created).hasSize(2);
        assertThat(created).extracting(PersonEntity::getEmail)
                .containsExactlyInAnyOrder("alice@test.com", "bob@test.com");
    }

    @Test
    @DisplayName("findAllByCreatedById — повертає порожній список для adminа без підопічних")
    void findAllByCreatedById_noCreatedUsers_returnsEmpty() {
        // admin не створював інших юзерів
        List<PersonEntity> created = personRepository.findAllByCreatedById(admin.getId());

        assertThat(created).isEmpty();
    }

    @Test
    @DisplayName("findAllByCreatedById — не повертає самого creator")
    void findAllByCreatedById_doesNotIncludeCreator() {
        List<PersonEntity> created = personRepository.findAllByCreatedById(creator.getId());

        assertThat(created).extracting(PersonEntity::getId)
                .doesNotContain(creator.getId());
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email, PersonRole role) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        p.setRole(role);
        return p;
    }

    private PersonEntity personCreatedBy(String email, PersonRole role, PersonEntity createdBy) {
        PersonEntity p = person(email, role);
        p.setCreatedBy(createdBy);
        return p;
    }
}
