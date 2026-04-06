-- liquibase formatted sql

-- changeset v.mishchuk:035
ALTER TABLE persons
ADD COLUMN provider VARCHAR(50) DEFAULT 'LOCAL' NOT NULL,
ADD COLUMN provider_id VARCHAR(255);
