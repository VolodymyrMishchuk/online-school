--liquibase formatted sql

--changeset antigravity:add-cascade-delete-to-enrollments-course-fk
--comment: Add ON DELETE CASCADE to enrollments course foreign key

ALTER TABLE enrollments DROP CONSTRAINT fk_enrollments_course;

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_course
    FOREIGN KEY (course_id)
    REFERENCES courses(id)
    ON DELETE CASCADE;
