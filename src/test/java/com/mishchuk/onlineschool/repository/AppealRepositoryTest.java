package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.AppealEntity;
import com.mishchuk.onlineschool.repository.entity.AppealStatus;
import com.mishchuk.onlineschool.repository.entity.ContactMethod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

class AppealRepositoryTest extends AbstractRepositoryTest {

    @Autowired private AppealRepository appealRepository;

    // ─────────────────────── findAllByOrderByCreatedAtDesc ───────────────────────

    @Test
    @DisplayName("findAllByOrderByCreatedAtDesc — повертає всі звернення відсортовані за createdAt DESC")
    void findAll_returnsSortedByCreatedAtDesc() {
        appealRepository.save(appeal("First appeal"));
        appealRepository.save(appeal("Second appeal"));
        appealRepository.save(appeal("Third appeal"));

        Page<AppealEntity> page = appealRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(3);

        // Перевіряємо сортування DESC (останній збережений — перший у результаті)
        // createdAt встановлюється @CreationTimestamp, тому більш пізні будуть першими
        assertThat(page.getContent()).isSortedAccordingTo(
                (a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())
        );
    }

    @Test
    @DisplayName("findAllByOrderByCreatedAtDesc — пагінація обмежує результати")
    void findAll_pagination_limitsResults() {
        for (int i = 1; i <= 5; i++) {
            appealRepository.save(appeal("Appeal " + i));
        }

        Page<AppealEntity> firstPage  = appealRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 3));
        Page<AppealEntity> secondPage = appealRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(1, 3));

        assertThat(firstPage.getContent()).hasSize(3);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(secondPage.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("findAllByOrderByCreatedAtDesc — повертає порожня сторінка якщо немає звернень")
    void findAll_noAppeals_returnsEmptyPage() {
        Page<AppealEntity> page = appealRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("findAllByOrderByCreatedAtDesc — одне звернення повертається коректно")
    void findAll_singleAppeal_returnsIt() {
        appealRepository.save(appeal("Single"));

        Page<AppealEntity> page = appealRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getMessage()).isEqualTo("Single");
    }

    // ─────────────────────── helpers ───────────────────────

    private AppealEntity appeal(String message) {
        AppealEntity a = new AppealEntity();
        a.setContactMethod(ContactMethod.EMAIL);
        a.setContactDetails("test@example.com");
        a.setMessage(message);
        a.setStatus(AppealStatus.NEW);
        return a;
    }
}
