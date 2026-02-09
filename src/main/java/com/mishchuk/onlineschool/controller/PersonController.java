package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/persons")
@RequiredArgsConstructor
public class PersonController {

    private final PersonService personService;

    @PostMapping
    public ResponseEntity<Void> createPerson(@RequestBody PersonCreateDto dto) {
        personService.createPerson(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonDto> getPerson(@PathVariable UUID id) {
        return personService.getPerson(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<PersonDto>> getAllPersons() {
        List<PersonDto> persons = personService.getAllPersons();
        if (persons.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(persons);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePerson(@PathVariable UUID id, @RequestBody PersonUpdateDto dto) {
        try {
            personService.updatePerson(id, dto);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            // In a real app, use @ControllerAdvice
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePerson(@PathVariable UUID id) {
        personService.deletePerson(id);
        return ResponseEntity.noContent().build();
    }

    // Users Management Endpoints

    @GetMapping("/with-enrollments")
    public ResponseEntity<List<com.mishchuk.onlineschool.controller.dto.PersonWithEnrollmentsDto>> getAllPersonsWithEnrollments() {
        return ResponseEntity.ok(personService.getAllPersonsWithEnrollments());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updatePersonStatus(@PathVariable UUID id, @RequestParam String status) {
        try {
            personService.updatePersonStatus(id, status);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/enrollments/{courseId}")
    public ResponseEntity<Void> addCourseAccess(@PathVariable UUID id, @PathVariable UUID courseId) {
        try {
            personService.addCourseAccess(id, courseId);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}/enrollments/{courseId}")
    public ResponseEntity<Void> removeCourseAccess(@PathVariable UUID id, @PathVariable UUID courseId) {
        personService.removeCourseAccess(id, courseId);
        return ResponseEntity.noContent().build();
    }
}
