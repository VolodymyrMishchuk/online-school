-- liquibase formatted sql

-- changeset volodymyr:034 context:main
ALTER TABLE appeals ALTER COLUMN user_id DROP NOT NULL;
ALTER TABLE appeals ADD COLUMN guest_name VARCHAR(255);
