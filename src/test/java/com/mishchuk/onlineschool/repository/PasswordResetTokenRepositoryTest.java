package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PasswordResetTokenEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenRepositoryTest extends AbstractRepositoryTest {

    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private PersonRepository             personRepository;

    private PersonEntity alice;

    @BeforeEach
    void setUp() {
        alice = personRepository.save(person("alice@test.com"));
    }

    // ─────────────────────── findByToken ───────────────────────

    @Test
    @DisplayName("findByToken — знаходить токен за значенням")
    void findByToken_found() {
        passwordResetTokenRepository.save(
                new PasswordResetTokenEntity(alice, "reset-abc-123", OffsetDateTime.now().plusHours(1)));

        Optional<PasswordResetTokenEntity> result = passwordResetTokenRepository.findByToken("reset-abc-123");

        assertThat(result).isPresent();
        assertThat(result.get().getUser().getId()).isEqualTo(alice.getId());
        assertThat(result.get().isUsed()).isFalse();
    }

    @Test
    @DisplayName("findByToken — повертає empty якщо токен не існує")
    void findByToken_notFound_returnsEmpty() {
        Optional<PasswordResetTokenEntity> result = passwordResetTokenRepository.findByToken("nonexistent-token");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByToken — повертає вже використаний токен (used=true)")
    void findByToken_usedToken_stillFound() {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                alice, "used-token-xyz", OffsetDateTime.now().plusHours(1));
        token.setUsed(true);
        passwordResetTokenRepository.save(token);

        Optional<PasswordResetTokenEntity> result = passwordResetTokenRepository.findByToken("used-token-xyz");

        assertThat(result).isPresent();
        assertThat(result.get().isUsed()).isTrue();
    }

    @Test
    @DisplayName("findByToken — повертає прострочений токен (перевірка expiryDate — на рівні сервісу)")
    void findByToken_expiredToken_stillReturned() {
        PasswordResetTokenEntity expired = new PasswordResetTokenEntity(
                alice, "expired-token", OffsetDateTime.now().minusHours(1));
        passwordResetTokenRepository.save(expired);

        Optional<PasswordResetTokenEntity> result = passwordResetTokenRepository.findByToken("expired-token");

        // Репозиторій не фільтрує по expiryDate — це відповідальність сервісу
        assertThat(result).isPresent();
        assertThat(result.get().getExpiryDate()).isBefore(OffsetDateTime.now());
    }

    @Test
    @DisplayName("findByToken — токен унікальний (unique constraint)")
    void token_uniqueConstraint_enforced() {
        passwordResetTokenRepository.save(
                new PasswordResetTokenEntity(alice, "unique-token", OffsetDateTime.now().plusHours(1)));
        passwordResetTokenRepository.flush();

        // Спроба зберегти дублюючий токен має викинути виключення
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> {
            passwordResetTokenRepository.save(
                    new PasswordResetTokenEntity(alice, "unique-token", OffsetDateTime.now().plusHours(2)));
            passwordResetTokenRepository.flush();
        });
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        return p;
    }
}
