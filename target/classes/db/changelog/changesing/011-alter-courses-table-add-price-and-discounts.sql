-- liquibase formatted sql

-- changeset antigravity:011-alter-courses-table-add-price-and-discounts
ALTER TABLE courses ADD COLUMN price DECIMAL(19, 2);
ALTER TABLE courses ADD COLUMN discount_amount DECIMAL(19, 2);
ALTER TABLE courses ADD COLUMN discount_percentage INTEGER;
