package com.mishchuk.onlineschool.repository;

import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Base class for all repository tests.
 *
 * Uses @DataJpaTest which:
 *  - Loads only JPA-related beans (entities, repositories)
 *  - Does NOT load controllers, services or security
 *  - Wraps each test in a transaction (auto-rollback after each test)
 *
 * Testcontainers is configured via the TC JDBC URL in application-test.yml:
 *  jdbc:tc:postgresql:15:///testdb  ← spins up a real PostgreSQL 15 container
 * Liquibase runs automatically against this container, applying all migrations.
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class AbstractRepositoryTest {
}
