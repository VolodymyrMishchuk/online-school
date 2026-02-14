-- liquibase formatted sql

-- changeset volodymyr.mishchuk:020-alter-notifications-add-button-url
ALTER TABLE notifications ADD COLUMN button_url VARCHAR(255);

-- changeset volodymyr.mishchuk:021-alter-notifications-update-type-enum
-- Note: Postgres enums might need explicit ALTER TYPE if implemented as DB enum. 
-- Assuming it's stored as VARCHAR based on Entity definition @Enumerated(EnumType.STRING).
-- If it is a VARCHAR column, no change needed for enum values at DB level unless there's a check constraint.
