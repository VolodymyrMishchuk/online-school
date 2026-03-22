-- liquibase formatted sql

-- changeset volodymyr.mishchuk:add-language-to-persons
ALTER TABLE persons
ADD COLUMN language VARCHAR(5) DEFAULT 'uk' NOT NULL;
