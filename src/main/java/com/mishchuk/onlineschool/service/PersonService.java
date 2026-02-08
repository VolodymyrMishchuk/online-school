package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PersonService {
    void createPerson(PersonCreateDto dto);

    Optional<PersonDto> getPerson(UUID id);

    List<PersonDto> getAllPersons();

    void updatePerson(UUID id, PersonUpdateDto dto);

    void deletePerson(UUID id);
}
