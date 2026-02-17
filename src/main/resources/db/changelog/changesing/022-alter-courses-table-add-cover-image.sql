--liquibase formatted sql

--changeset volodymyr.mishchuk:022
ALTER TABLE courses ADD COLUMN cover_image BYTEA;
