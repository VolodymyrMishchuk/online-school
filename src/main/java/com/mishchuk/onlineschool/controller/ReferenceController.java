package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PersonStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/references")
public class ReferenceController {

    @GetMapping("/roles")
    public ResponseEntity<List<PersonRole>> getRoles() {
        return ResponseEntity.ok(Arrays.asList(PersonRole.values()));
    }

    @GetMapping("/statuses")
    public ResponseEntity<List<PersonStatus>> getStatuses() {
        return ResponseEntity.ok(Arrays.asList(PersonStatus.values()));
    }
}
