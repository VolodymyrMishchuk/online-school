package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.RefreshTokenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenRepositoryTest extends AbstractRepositoryTest {

    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PersonRepository       personRepository;

    private PersonEntity alice;
    private PersonEntity bob;

    @BeforeEach
    void setUp() {
        alice = personRepository.save(person("alice@test.com"));
        bob   = personRepository.save(person("bob@test.com"));
    }

    // ─────────────────────── findByToken ───────────────────────

    @Test
    @DisplayName("findByToken — знаходить токен за значенням")
    void findByToken_found() {
        refreshTokenRepository.save(token(alice.getId(), "my.refresh.token", OffsetDateTime.now().plusDays(7)));

        Optional<RefreshTokenEntity> result = refreshTokenRepository.findByToken("my.refresh.token");

        assertThat(result).isPresent();
        assertThat(result.get().getPersonId()).isEqualTo(alice.getId());
    }

    @Test
    @DisplayName("findByToken — повертає empty якщо токен не існує")
    void findByToken_notFound_returnsEmpty() {
        Optional<RefreshTokenEntity> result = refreshTokenRepository.findByToken("ghost.token");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByToken — не повертає токен іншого користувача")
    void findByToken_wrongToken_returnsEmpty() {
        refreshTokenRepository.save(token(alice.getId(), "alice.token", OffsetDateTime.now().plusDays(7)));

        Optional<RefreshTokenEntity> result = refreshTokenRepository.findByToken("bob.token");

        assertThat(result).isEmpty();
    }

    // ─────────────────────── deleteByPersonId ───────────────────────

    @Test
    @DisplayName("deleteByPersonId — видаляє всі токени конкретного користувача")
    void deleteByPersonId_deletesAllForPerson() {
        refreshTokenRepository.save(token(alice.getId(), "alice.token.1", OffsetDateTime.now().plusDays(7)));
        refreshTokenRepository.save(token(alice.getId(), "alice.token.2", OffsetDateTime.now().plusDays(7)));
        refreshTokenRepository.save(token(bob.getId(),   "bob.token.1",   OffsetDateTime.now().plusDays(7)));

        refreshTokenRepository.deleteByPersonId(alice.getId());
        refreshTokenRepository.flush();

        assertThat(refreshTokenRepository.findByToken("alice.token.1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("alice.token.2")).isEmpty();
        // Bob's token залишається
        assertThat(refreshTokenRepository.findByToken("bob.token.1")).isPresent();
    }

    @Test
    @DisplayName("deleteByPersonId — не падає якщо токенів немає")
    void deleteByPersonId_noTokens_noException() {
        refreshTokenRepository.deleteByPersonId(UUID.randomUUID());
        // просто перевіряємо що немає виключення
        assertThat(refreshTokenRepository.count()).isZero();
    }

    // ─────────────────────── deleteByExpiryDateBefore ───────────────────────

    @Test
    @DisplayName("deleteByExpiryDateBefore — видаляє прострочені токени")
    void deleteByExpiryDateBefore_deletesExpired() {
        refreshTokenRepository.save(token(alice.getId(), "expired.token", OffsetDateTime.now().minusDays(1)));
        refreshTokenRepository.save(token(bob.getId(),   "valid.token",   OffsetDateTime.now().plusDays(7)));

        refreshTokenRepository.deleteByExpiryDateBefore(OffsetDateTime.now());
        refreshTokenRepository.flush();

        assertThat(refreshTokenRepository.findByToken("expired.token")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("valid.token")).isPresent();
    }

    @Test
    @DisplayName("deleteByExpiryDateBefore — не видаляє валідні токени")
    void deleteByExpiryDateBefore_keepsValidTokens() {
        refreshTokenRepository.save(token(alice.getId(), "valid.1", OffsetDateTime.now().plusDays(1)));
        refreshTokenRepository.save(token(bob.getId(),   "valid.2", OffsetDateTime.now().plusDays(7)));

        refreshTokenRepository.deleteByExpiryDateBefore(OffsetDateTime.now());
        refreshTokenRepository.flush();

        assertThat(refreshTokenRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("deleteByExpiryDateBefore — токен рівно на межі НЕ видаляється (exclusive)")
    void deleteByExpiryDateBefore_boundary_excluded() {
        OffsetDateTime boundary = OffsetDateTime.now().plusSeconds(5);
        refreshTokenRepository.save(token(alice.getId(), "boundary.token", boundary));

        // видаляємо все до boundary — сам boundary не має потрапити
        refreshTokenRepository.deleteByExpiryDateBefore(boundary);
        refreshTokenRepository.flush();

        assertThat(refreshTokenRepository.findByToken("boundary.token")).isPresent();
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        return p;
    }

    private RefreshTokenEntity token(UUID personId, String tokenValue, OffsetDateTime expiryDate) {
        return RefreshTokenEntity.builder()
                .token(tokenValue)
                .personId(personId)
                .expiryDate(expiryDate)
                .build();
    }
}
