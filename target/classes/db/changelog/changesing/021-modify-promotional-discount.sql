-- liquibase formatted sql

-- changeset antigravity:021-modify-promotional-discount
ALTER TABLE courses RENAME COLUMN promotional_discount TO promotional_discount_percentage;
ALTER TABLE courses ALTER COLUMN promotional_discount_percentage TYPE INTEGER USING promotional_discount_percentage::INTEGER;
ALTER TABLE courses ADD COLUMN promotional_discount_amount DECIMAL(19, 2);
