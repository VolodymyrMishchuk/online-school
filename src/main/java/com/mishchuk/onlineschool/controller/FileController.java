package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.exception.ResourceNotFoundException;
import com.mishchuk.onlineschool.repository.PersonRepository;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileStorageService fileStorageService;
    private final PersonRepository personRepository;

    @PostMapping("/upload")
    public ResponseEntity<FileDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @AuthenticationPrincipal UserDetails userDetails) {
        // In real implementation, get PersonEntity from userDetails
        // For now, we'll skip this and you'll need to implement getCurrentUser()
        PersonEntity currentUser = null;
        if (userDetails != null) {
            currentUser = getCurrentUser(userDetails);
        }

        FileDto fileDto = fileStorageService.uploadFile(file, entityType, entityId, currentUser);
        return ResponseEntity.ok(fileDto);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {
        FileStorageService.FileDownloadDto download = fileStorageService.downloadFile(fileId);

        InputStreamResource resource = new InputStreamResource(download.inputStream());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.metadata().getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(download.metadata().getContentType()))
                .contentLength(download.metadata().getFileSize())
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID fileId) {
        fileStorageService.deleteFile(fileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    public ResponseEntity<List<FileDto>> getFilesForEntity(
            @PathVariable String entityType,
            @PathVariable UUID entityId) {
        List<FileDto> files = fileStorageService.getFilesForEntity(entityType, entityId);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/my-files")
    public ResponseEntity<List<FileDto>> getMyFiles(
            @AuthenticationPrincipal UserDetails userDetails) {
        PersonEntity currentUser = getCurrentUser(userDetails);
        List<FileDto> files = fileStorageService.getFilesByUser(currentUser);
        return ResponseEntity.ok(files);
    }

    private PersonEntity getCurrentUser(UserDetails userDetails) {
        return personRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
