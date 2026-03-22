-- liquibase formatted sql

-- changeset v.mishchuk:25
ALTER TABLE courses ADD COLUMN created_by_id UUID;
ALTER TABLE courses ADD CONSTRAINT fk_courses_created_by FOREIGN KEY (created_by_id) REFERENCES persons(id) ON DELETE SET NULL;

ALTER TABLE modules ADD COLUMN created_by_id UUID;
ALTER TABLE modules ADD CONSTRAINT fk_modules_created_by FOREIGN KEY (created_by_id) REFERENCES persons(id) ON DELETE SET NULL;

ALTER TABLE lessons ADD COLUMN created_by_id UUID;
ALTER TABLE lessons ADD CONSTRAINT fk_lessons_created_by FOREIGN KEY (created_by_id) REFERENCES persons(id) ON DELETE SET NULL;
