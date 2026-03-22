-- liquibase formatted sql

-- changeset antigravity:010-alter-courses-table-add-access-duration
ALTER TABLE courses ADD COLUMN access_duration INT;
