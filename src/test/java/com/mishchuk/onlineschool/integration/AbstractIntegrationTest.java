package com.mishchuk.onlineschool.integration;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Base class for all integration tests.
 *
 * Strategy:
 *  - @SpringBootTest loads the FULL application context (all beans, security, JPA)
 *  - Testcontainers PostgreSQL is configured via TC JDBC URL in application-test.yml
 *  - @AutoConfigureMockMvc injects MockMvc to call real HTTP endpoints
 *  - MinioClient & JavaMailSender are mocked to avoid real external connections
 *  - Each test class manages its own data; no @Transactional (HTTP calls go throgh
 *    the full Spring TX boundary — rollback would not work correctly)
 *
 * Security:
 *  - Integration tests go through the real JWT filter chain
 *  - Use AuthHelper to register/login and obtain a real Bearer token
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Mocked to prevent MinioClient from trying to connect to a real MinIO instance
     * on application startup. Individual tests that test file upload would need
     * to configure behaviour via Mockito.
     */
    @MockBean
    protected MinioClient minioClient;
}
