--liquibase formatted sql

--changeset volodymyr.mishchuk:023-create-course-covers-table
CREATE TABLE course_covers (
    course_id UUID PRIMARY KEY,
    image_data BYTEA,
    average_color VARCHAR(7),
    CONSTRAINT fk_course_cover_course FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE
);

-- Migrate existing data
INSERT INTO course_covers (course_id, image_data)
SELECT id, cover_image FROM courses WHERE cover_image IS NOT NULL;

-- Drop old column
ALTER TABLE courses DROP COLUMN cover_image;
