package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.controller.dto.PersonWithEnrollmentsDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonService {
    void createPerson(PersonCreateDto dto);

    Optional<PersonDto> getPerson(UUID id);

    List<PersonDto> getAllPersons();

    void updatePerson(UUID id, PersonUpdateDto dto);

    void deletePerson(UUID id);

    // Users Management
    List<PersonWithEnrollmentsDto> getAllPersonsWithEnrollments();

    void updatePersonStatus(UUID id, String status);

    void addCourseAccess(UUID personId, UUID courseId);

    void removeCourseAccess(UUID personId, UUID courseId);
}
