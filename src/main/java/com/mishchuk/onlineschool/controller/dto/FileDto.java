package com.mishchuk.onlineschool.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDto {
    private UUID id;
    private String fileName;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime uploadedAt;
    private String downloadUrl;
    private String relatedEntityType;
    private UUID relatedEntityId;
}
