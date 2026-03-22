package com.mishchuk.onlineschool.service;

import io.minio.StatObjectResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface MinioService {
    String uploadFile(MultipartFile file, String folder) throws Exception;
    InputStream downloadFile(String objectName) throws Exception;
    void deleteFile(String objectName) throws Exception;
    StatObjectResponse getFileMetadata(String objectName) throws Exception;
}
