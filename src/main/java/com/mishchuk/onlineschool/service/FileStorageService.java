package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.repository.entity.FileEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

public interface FileStorageService {
    FileDto uploadFile(MultipartFile file, String entityType, UUID entityId, PersonEntity uploadedBy);
    FileDownloadDto downloadFile(UUID fileId);
    void deleteFile(UUID fileId);
    List<FileDto> getFilesForEntity(String entityType, UUID entityId);
    List<FileDto> getFilesByUser(PersonEntity person);
    List<FileDto> getLessonFiles(UUID lessonId);
    List<FileDto> getLessonFilesOrdered(UUID lessonId);

    record FileDownloadDto(InputStream inputStream, FileEntity metadata) {}
}
