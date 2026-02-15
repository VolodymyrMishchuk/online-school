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
import java.util.UUID;

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
    public ResponseEntity<ModuleDto> getModule(@PathVariable UUID id) {
        return moduleService.getModule(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<ModuleDto>> getAllModules(@RequestParam(required = false) UUID courseId) {
        List<ModuleDto> modules = moduleService.getAllModules(courseId);
        return ResponseEntity.ok(modules);
    }

    @GetMapping("/{id}/lessons")
    public ResponseEntity<List<com.mishchuk.onlineschool.controller.dto.LessonDto>> getModuleLessons(
            @PathVariable UUID id) {
        List<com.mishchuk.onlineschool.controller.dto.LessonDto> lessons = moduleService.getModuleLessons(id);
        return ResponseEntity.ok(lessons);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateModule(@PathVariable UUID id, @RequestBody ModuleUpdateDto dto) {
        try {
            moduleService.updateModule(id, dto);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteModule(@PathVariable UUID id) {
        moduleService.deleteModule(id);
        return ResponseEntity.noContent().build();
    }
}
