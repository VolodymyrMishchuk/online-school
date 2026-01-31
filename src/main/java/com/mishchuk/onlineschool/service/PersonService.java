package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;

import java.util.List;
import java.util.Optional;

public interface PersonService {
    void createPerson(PersonCreateDto dto);

    Optional<PersonDto> getPerson(java.util.UUID id);

    List<PersonDto> getAllPersons();

    void updatePerson(java.util.UUID id, PersonUpdateDto dto);

    void deletePerson(java.util.UUID id);
}
