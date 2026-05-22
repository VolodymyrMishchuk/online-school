--liquibase formatted sql

--changeset vmishchuk:039-add-blocked-course-feature-flags
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS extend_for_review_enabled  BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS renewal_enabled             BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN IF NOT EXISTS next_course_discount_enabled BOOLEAN NOT NULL DEFAULT TRUE;
