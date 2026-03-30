package com.mishchuk.onlineschool.controller;

import com.mishchuk.onlineschool.controller.dto.AppealCreateRequest;
import com.mishchuk.onlineschool.controller.dto.AppealResponse;
import com.mishchuk.onlineschool.repository.entity.AppealStatus;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.security.CustomUserDetailsService;
import com.mishchuk.onlineschool.service.AppealService;
import com.mishchuk.onlineschool.controller.dto.PublicAppealCreateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/appeals")
@RequiredArgsConstructor
public class AppealController {

    private final AppealService appealService;
    private final CustomUserDetailsService userDetailsService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppealResponse> createAppeal(
            @Valid @ModelAttribute AppealCreateRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos,
            Principal principal) {

        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        PersonEntity person = userDetailsService.getPerson(principal.getName());
        AppealResponse response = appealService.createAppeal(person.getId(), request, photos);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/public", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AppealResponse> createPublicAppeal(
            @Valid @ModelAttribute PublicAppealCreateRequest request,
            @RequestPart(value = "photos", required = false) List<MultipartFile> photos) {

        AppealResponse response = appealService.createPublicAppeal(request, photos);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AppealResponse>> getAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<AppealResponse> appeals = appealService.getAppeals(pageable);
        return ResponseEntity.ok(appeals);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppealResponse> getAppeal(@PathVariable UUID id) {
        return ResponseEntity.ok(appealService.getAppeal(id));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppealResponse> updateAppealStatus(
            @PathVariable UUID id,
            @RequestParam AppealStatus status) {
        return ResponseEntity.ok(appealService.updateAppealStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAppeal(@PathVariable UUID id) {
        appealService.deleteAppeal(id);
        return ResponseEntity.noContent().build();
    }
}
