-- liquibase formatted sql

-- changeset v.mishchuk:036
ALTER TABLE persons ADD COLUMN avatar_url VARCHAR(1024);
