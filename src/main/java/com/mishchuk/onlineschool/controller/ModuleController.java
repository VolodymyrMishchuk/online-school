package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.service.ModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/modules")
@RequiredArgsConstructor
public class ModuleController {

    private final ModuleService moduleService;

    @PostMapping
    public ResponseEntity<Void> createModule(@RequestBody ModuleCreateDto dto) {
        moduleService.createModule(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<ModuleDto> getModule(@PathVariable java.util.UUID id) {
        return moduleService.getModule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ModuleDto>> getAllModules() {
        List<ModuleDto> modules = moduleService.getAllModules();
        if (modules.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(modules);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateModule(@PathVariable java.util.UUID id, @RequestBody ModuleUpdateDto dto) {
        try {
            moduleService.updateModule(id, dto);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable java.util.UUID id) {
        moduleService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }
}
