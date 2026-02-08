package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.controller.dto.LessonCreateDto;
import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.LessonUpdateDto;
import com.mishchuk.onlineschool.service.FileStorageService;
import com.mishchuk.onlineschool.service.LessonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final FileStorageService fileStorageService;

    @PostMapping
    public ResponseEntity<LessonDto> createLesson(@RequestBody LessonCreateDto dto) {
        LessonDto createdLesson = lessonService.createLesson(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdLesson);
    }

    @GetMapping
    public ResponseEntity<List<LessonDto>> getAllLessons() {
        List<LessonDto> lessons = lessonService.getAllLessons();
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/unassigned")
    public ResponseEntity<List<LessonDto>> getUnassignedLessons() {
        List<LessonDto> lessons = lessonService.getUnassignedLessons();
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/{id}")
    public ResponseEntity<LessonDto> getLesson(@PathVariable UUID id) {
        return lessonService.getLesson(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<List<FileDto>> getLessonFiles(@PathVariable UUID id) {
        List<FileDto> files = fileStorageService.getLessonFiles(id);
        return ResponseEntity.ok(files);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateLesson(@PathVariable UUID id, @RequestBody LessonUpdateDto dto) {
        try {
            lessonService.updateLesson(id, dto);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLesson(@PathVariable UUID id) {
        lessonService.deleteLesson(id);
        return ResponseEntity.noContent().build();
    }
}
