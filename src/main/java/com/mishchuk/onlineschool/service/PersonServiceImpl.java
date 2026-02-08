package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.exception.EmailAlreadyExistsException;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.mapper.PersonMapper;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void createPerson(PersonCreateDto dto) {
        log.info("Attempting to register new user with email: {}", dto.email());

        if (personRepository.findByEmail(dto.email()).isPresent()) {
            log.warn("Registration failed: email {} already exists", dto.email());
            throw new EmailAlreadyExistsException("Email already exists: " + dto.email());
        }

        PersonEntity entity = personMapper.toEntity(dto);
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        personRepository.save(entity);

        log.info("Successfully registered user with email: {} and ID: {}", dto.email(), entity.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PersonDto> getPerson(UUID id) {
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
    public void updatePerson(UUID id, PersonUpdateDto dto) {
        PersonEntity entity = personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Person not found with id: " + id));
        personMapper.updateEntityFromDto(dto, entity);
        personRepository.save(entity);
    }

    @Override
    @Transactional
    public void deletePerson(UUID id) {
        personRepository.deleteById(id);
    }
}
