-- liquibase formatted sql

-- changeset v.mishchuk:26
ALTER TABLE persons ADD COLUMN created_by_id UUID;
ALTER TABLE persons ADD CONSTRAINT fk_persons_created_by FOREIGN KEY (created_by_id) REFERENCES persons(id) ON DELETE SET NULL;
