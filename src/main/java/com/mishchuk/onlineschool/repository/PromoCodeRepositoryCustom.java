package com.mishchuk.onlineschool.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.mishchuk.onlineschool.repository.entity.PromoCodeEntity;

public interface PromoCodeRepositoryCustom {
    Page<PromoCodeEntity> findPaginatedPromoCodes(
            String search,
            String sortKey,
            String sortDir,
            String statusSort,
            Pageable pageable,
            java.util.UUID creatorId
    );
}
