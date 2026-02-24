package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.CourseStatus;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final PersonRepository personRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'FAKE_ADMIN')")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createCourse(
            @RequestPart("course") CourseCreateDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        courseService.createCourse(dto, image);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseDto> getCourse(@PathVariable UUID id) {
        return courseService.getCourse(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<CourseDto>> getAllCourses(
            @RequestParam(required = false) UUID userId,
            java.security.Principal principal) {

        List<CourseDto> courses;
        if (userId != null) {
            // Check if user is authenticated
            if (principal == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }

            // Check if requesting own data or is Admin
            com.mishchuk.onlineschool.repository.entity.PersonEntity currentUser = personRepository
                    .findByEmail(principal.getName())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean isAdmin = currentUser.getRole() == PersonRole.ADMIN;

            if (!currentUser.getId().equals(userId) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            courses = courseService.getAllCoursesWithEnrollment(userId);
        } else {
            courses = courseService.getAllCourses();
        }

        if (courses.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(courses);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FAKE_ADMIN')")
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCourse(
            @PathVariable UUID id,
            @RequestPart("course") CourseUpdateDto dto,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        try {
            courseService.updateCourse(id, dto, image);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/cover")
    public ResponseEntity<byte[]> getCourseCover(@PathVariable UUID id) {
        return courseService.getCourseCoverImage(id)
                .map(bytes -> ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(bytes))
                .orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FAKE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCourse(@PathVariable UUID id) {
        courseService.deleteCourse(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/extend-access")
    public ResponseEntity<Void> extendAccessForReview(
            @PathVariable UUID id,
            @RequestParam("video") MultipartFile video,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get person from authenticated user
            PersonEntity person = personRepository
                    .findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create directory if not exists
            String uploadDir = "uploads/reviews/" + person.getId();
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Save file
            String filename = UUID.randomUUID() + "_" + video.getOriginalFilename();
            Path filePath = uploadPath.resolve(filename);
            Files.copy(video.getInputStream(), filePath,
                    StandardCopyOption.REPLACE_EXISTING);

            // Construct URL (Local)
            String videoUrl = "/uploads/reviews/" + person.getId() + "/" + filename;

            courseService.extendAccessForReview(person.getId(), id, videoUrl,
                    video.getOriginalFilename());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FAKE_ADMIN')")
    @PostMapping("/{id}/clone")
    public ResponseEntity<Void> cloneCourse(@PathVariable UUID id) {
        courseService.cloneCourse(id);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'FAKE_ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateCourseStatus(
            @PathVariable UUID id,
            @RequestParam CourseStatus status) {
        courseService.updateCourseStatus(id, status);
        return ResponseEntity.ok().build();
    }
}
