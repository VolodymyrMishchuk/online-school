package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.NotificationEntity;
import com.mishchuk.onlineschool.repository.entity.NotificationType;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationRepositoryTest extends AbstractRepositoryTest {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private PersonRepository personRepository;

    private PersonEntity alice;
    private PersonEntity bob;

    @BeforeEach
    void setUp() {
        alice = personRepository.save(person("alice@test.com"));
        bob   = personRepository.save(person("bob@test.com"));
    }

    // ─────────────────────── findByRecipientIdOrderByCreatedAtDesc (Page) ───────────────────────

    @Test
    @DisplayName("findByRecipientIdOrderByCreatedAtDesc — повертає лише нотифікації конкретного отримувача")
    void findByRecipientPaged_returnsOnlyForThatRecipient() {
        save(notification(alice, "A1"));
        save(notification(alice, "A2"));
        save(notification(bob,   "B1"));

        Page<NotificationEntity> page = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(alice.getId(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(n -> n.getRecipient().getId())
                .containsOnly(alice.getId());
    }

    @Test
    @DisplayName("findByRecipientIdOrderByCreatedAtDesc — порожня сторінка якщо нотифікацій немає")
    void findByRecipientPaged_noNotifications_returnsEmpty() {
        Page<NotificationEntity> page = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(alice.getId(), PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findByRecipientIdOrderByCreatedAtDesc — пагінація обмежує кількість результатів")
    void findByRecipientPaged_pagination_limitsResults() {
        for (int i = 1; i <= 5; i++) {
            save(notification(alice, "Msg " + i));
        }

        Page<NotificationEntity> page = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(alice.getId(), PageRequest.of(0, 3));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    // ─────────────────────── findByRecipientIdOrderByCreatedAtDesc (List) ───────────────────────

    @Test
    @DisplayName("findByRecipientId (List) — повертає всі нотифікації без пагінації")
    void findByRecipientList_returnsAll() {
        save(notification(alice, "A1"));
        save(notification(alice, "A2"));
        save(notification(bob,   "B1"));

        List<NotificationEntity> result = notificationRepository
                .findByRecipientIdOrderByCreatedAtDesc(alice.getId());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(n -> n.getRecipient().getId())
                .containsOnly(alice.getId());
    }

    // ─────────────────────── countByRecipientIdAndIsReadFalse ───────────────────────

    @Test
    @DisplayName("countByRecipientIdAndIsReadFalse — рахує лише непрочитані нотифікації")
    void countUnread_countsOnlyUnread() {
        save(notification(alice, "U1"));
        save(notification(alice, "U2"));
        NotificationEntity read    = notification(alice, "R1");
        read.setRead(true);
        save(read);
        save(notification(bob, "B1")); // інший отримувач

        long count = notificationRepository
                .countByRecipientIdAndIsReadFalse(alice.getId());

        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("countByRecipientIdAndIsReadFalse — повертає 0 якщо всі прочитані")
    void countUnread_allRead_returnsZero() {
        NotificationEntity n = notification(alice, "R1");
        n.setRead(true);
        save(n);

        long count = notificationRepository
                .countByRecipientIdAndIsReadFalse(alice.getId());

        assertThat(count).isZero();
    }

    @Test
    @DisplayName("countByRecipientIdAndIsReadFalse — повертає 0 якщо нотифікацій немає")
    void countUnread_noNotifications_returnsZero() {
        long count = notificationRepository
                .countByRecipientIdAndIsReadFalse(alice.getId());

        assertThat(count).isZero();
    }

    // ─────────────────────── helpers ───────────────────────

    private PersonEntity person(String email) {
        PersonEntity p = new PersonEntity();
        p.setEmail(email);
        p.setPassword("pass");
        return p;
    }

    private NotificationEntity notification(PersonEntity recipient, String title) {
        NotificationEntity n = new NotificationEntity();
        n.setRecipient(recipient);
        n.setTitle(title);
        n.setMessage("Message for " + title);
        n.setType(NotificationType.GENERIC);
        return n;
    }

    private NotificationEntity save(NotificationEntity n) {
        return notificationRepository.save(n);
    }
}
