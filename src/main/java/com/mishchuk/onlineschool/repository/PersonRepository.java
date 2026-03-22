package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PersonEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.mishchuk.onlineschool.repository.entity.PersonRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, UUID>, PersonRepositoryCustom {
    Optional<PersonEntity> findByEmail(String email);

    List<PersonEntity> findAllByRole(PersonRole role);

    List<PersonEntity> findAllByCreatedById(UUID createdById);
}
