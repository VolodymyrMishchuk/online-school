package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.controller.dto.PersonWithEnrollmentsDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonService {
    void createPerson(PersonCreateDto dto);

    Optional<PersonDto> getPerson(UUID id);

    List<PersonDto> getAllPersons();

    void updatePerson(UUID id, PersonUpdateDto dto);
    void updateLanguage(UUID id, String language);

    void deletePerson(UUID id);

    List<PersonWithEnrollmentsDto> getAllPersonsWithEnrollments();

    Page<PersonWithEnrollmentsDto> getPaginatedPersons(
            String search,
            String sortKey,
            String sortDir,
            String blockedSort,
            String adminSort,
            Pageable pageable);

    void updatePersonStatus(UUID id, String status);

    void addCourseAccess(UUID personId, UUID courseId);

    void removeCourseAccess(UUID personId, UUID courseId);

    void changePassword(UUID personId, String oldPassword, String newPassword);
    
    void addPassword(UUID personId, String newPassword);
}
