package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends JpaRepository<PersonEntity, java.util.UUID> {
    Optional<PersonEntity> findByEmail(String email);

    java.util.List<PersonEntity> findAllByRole(com.mishchuk.onlineschool.repository.entity.PersonRole role);
}
