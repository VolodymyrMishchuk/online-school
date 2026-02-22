package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final com.mishchuk.onlineschool.repository.PersonRepository personRepository;

    @PreAuthorize("hasAnyRole('ADMIN', 'FAKE_ADMIN')")
    @PostMapping(consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> createCourse(
            @RequestPart("course") CourseCreateDto dto,
            @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image) {
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

            boolean isAdmin = currentUser.getRole() == com.mishchuk.onlineschool.repository.entity.PersonRole.ADMIN;

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
    @PutMapping(value = "/{id}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> updateCourse(
            @PathVariable UUID id,
            @RequestPart("course") CourseUpdateDto dto,
            @RequestPart(value = "image", required = false) org.springframework.web.multipart.MultipartFile image) {
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
                        .contentType(org.springframework.http.MediaType.IMAGE_JPEG)
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
            @RequestParam("video") org.springframework.web.multipart.MultipartFile video,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        try {
            // Get person from authenticated user
            com.mishchuk.onlineschool.repository.entity.PersonEntity person = personRepository
                    .findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Create directory if not exists
            String uploadDir = "uploads/reviews/" + person.getId();
            java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }

            // Save file
            String filename = java.util.UUID.randomUUID() + "_" + video.getOriginalFilename();
            java.nio.file.Path filePath = uploadPath.resolve(filename);
            java.nio.file.Files.copy(video.getInputStream(), filePath,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);

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
}
