package com.mishchuk.onlineschool.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;

public interface PersonRepositoryCustom {
    Page<PersonEntity> findPaginatedUsers(
            String search,
            String sortKey,
            String sortDir,
            String blockedSort, // "top", "bottom", or null
            String adminSort,   // "top", "bottom", or null
            Pageable pageable
    );
}
