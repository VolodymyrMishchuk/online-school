package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.config.MinioConfig;
import io.minio.*;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioServiceImplTest {

    @Mock private MinioClient minioClient;

    @InjectMocks
    private MinioServiceImpl minioService;

    @Captor private ArgumentCaptor<BucketExistsArgs> bucketExistsCaptor;
    @Captor private ArgumentCaptor<MakeBucketArgs> makeBucketCaptor;
    @Captor private ArgumentCaptor<PutObjectArgs> putObjectCaptor;
    @Captor private ArgumentCaptor<GetObjectArgs> getObjectCaptor;
    @Captor private ArgumentCaptor<RemoveObjectArgs> removeObjectCaptor;
    @Captor private ArgumentCaptor<StatObjectArgs> statObjectCaptor;

    @BeforeEach
    void setUp() {
        MinioConfig config = new MinioConfig();
        config.setBucketName("test-bucket");
        ReflectionTestUtils.setField(minioService, "minioConfig", config);
        
        ((Logger) LoggerFactory.getLogger(MinioServiceImpl.class)).setLevel(Level.OFF);
    }

    @AfterEach
    void clearSecurityContext() {
        ((Logger) LoggerFactory.getLogger(MinioServiceImpl.class)).setLevel(null);
    }

    // ─────────────────────── init ───────────────────────

    @Test
    @DisplayName("init — створює bucket якщо він не існує")
    void init_bucketDoesNotExist_createsBucket() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        minioService.init();

        verify(minioClient).makeBucket(makeBucketCaptor.capture());
        assertThat(makeBucketCaptor.getValue().bucket()).isEqualTo("test-bucket");
    }

    @Test
    @DisplayName("init — не створює bucket якщо він вже існує")
    void init_bucketExists_doesNotCreate() throws Exception {
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        minioService.init();

        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    @Test
    @DisplayName("init — глушить помилки ініціалізації")
    void init_throwsException_handledByCatch() throws Exception {
        when(minioClient.bucketExists(any())).thenThrow(new RuntimeException("MinIO down"));

        minioService.init();

        // No exception thrown up, logs handled it
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
    }

    // ─────────────────────── uploadFile ───────────────────────

    @Test
    @DisplayName("uploadFile — з папкою генерує правильний шлях і завантажує")
    void uploadFile_withFolder_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        String resultPath = minioService.uploadFile(file, "lessons");

        assertThat(resultPath).startsWith("lessons/");
        assertThat(resultPath).endsWith("_test.pdf");

        verify(minioClient).putObject(putObjectCaptor.capture());
        PutObjectArgs args = putObjectCaptor.getValue();
        assertThat(args.bucket()).isEqualTo("test-bucket");
        assertThat(args.object()).isEqualTo(resultPath);
    }

    @Test
    @DisplayName("uploadFile — без папки генерує правильний шлях (без '/')")
    void uploadFile_noFolder_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "note.txt", "text/plain", "abc".getBytes());

        String resultPath = minioService.uploadFile(file, null);

        assertThat(resultPath).doesNotContain("/");
        assertThat(resultPath).endsWith("_note.txt");

        verify(minioClient).putObject(putObjectCaptor.capture());
        assertThat(putObjectCaptor.getValue().object()).isEqualTo(resultPath);
    }

    @Test
    @DisplayName("uploadFile — пробрасує Exception якщо MinIO кидає помилку")
    void uploadFile_minioThrows_propagatesException() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "bad.pdf", "application/pdf", "data".getBytes());
        when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("MinIO unavailable"));

        assertThatThrownBy(() -> minioService.uploadFile(file, "lessons"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("MinIO unavailable");
    }

    // ─────────────────────── downloadFile ───────────────────────

    @Test
    @DisplayName("downloadFile — повертає InputStream з Minio")
    void downloadFile_success() throws Exception {
        InputStream stream = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
        
        // MinioClient.getObject returns GetObjectResponse (extends InputStream).
        // We cast mock to GetObjectResponse as it is a concrete class that extends InputStream.
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn((GetObjectResponse) mock(GetObjectResponse.class));

        InputStream result = minioService.downloadFile("lessons/file.pdf");

        assertThat(result).isNotNull();
        verify(minioClient).getObject(getObjectCaptor.capture());
        assertThat(getObjectCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(getObjectCaptor.getValue().object()).isEqualTo("lessons/file.pdf");
    }

    // ─────────────────────── deleteFile ───────────────────────

    @Test
    @DisplayName("deleteFile — викликає removeObject")
    void deleteFile_success() throws Exception {
        minioService.deleteFile("file.png");

        verify(minioClient).removeObject(removeObjectCaptor.capture());
        assertThat(removeObjectCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(removeObjectCaptor.getValue().object()).isEqualTo("file.png");
    }

    @Test
    @DisplayName("deleteFile — пробрасовує Exception якщо MinIO кидає помилку")
    void deleteFile_minioThrows_propagatesException() throws Exception {
        doThrow(new RuntimeException("MinIO delete error")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThatThrownBy(() -> minioService.deleteFile("file.png"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("MinIO delete error");
    }

    // ─────────────────────── getFileMetadata ───────────────────────

    @Test
    @DisplayName("getFileMetadata — повертає StatObjectResponse")
    void getFileMetadata_success() throws Exception {
        StatObjectResponse response = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(response);

        StatObjectResponse result = minioService.getFileMetadata("video.mp4");

        assertThat(result).isSameAs(response);
        verify(minioClient).statObject(statObjectCaptor.capture());
        assertThat(statObjectCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(statObjectCaptor.getValue().object()).isEqualTo("video.mp4");
    }
}
