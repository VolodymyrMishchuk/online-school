--liquibase formatted sql

--changeset vmishchuk:038-add-renewal-discount-to-courses
ALTER TABLE courses
    ADD COLUMN IF NOT EXISTS renewal_discount_percentage INTEGER,
    ADD COLUMN IF NOT EXISTS renewal_discount_amount NUMERIC(10, 2);
