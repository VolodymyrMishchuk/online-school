--liquibase formatted sql

--changeset author:mishchuk
--comment: Add ON DELETE CASCADE to enrollments student foreign key

ALTER TABLE enrollments DROP CONSTRAINT fk_enrollments_student;

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_student
    FOREIGN KEY (student_id)
    REFERENCES persons(id)
    ON DELETE CASCADE;
