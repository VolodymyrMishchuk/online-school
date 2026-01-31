package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.mapper.PersonMapper;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void createPerson(PersonCreateDto dto) {
        PersonEntity entity = personMapper.toEntity(dto);
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        personRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonDto> getPerson(java.util.UUID id) {
        return personRepository.findById(id)
                .map(personMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonDto> getAllPersons() {
        return personRepository.findAll().stream()
                .map(personMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updatePerson(java.util.UUID id, PersonUpdateDto dto) {
        PersonEntity entity = personRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Person not found"));
        personMapper.updateEntityFromDto(dto, entity);
        personRepository.save(entity);
    }

    @Override
    @Transactional
    public void deletePerson(java.util.UUID id) {
        personRepository.deleteById(id);
    }
}
