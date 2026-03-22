--liquibase formatted sql
--changeset volodymyr:021
ALTER TABLE courses ADD COLUMN promotional_discount_amount DECIMAL(19, 2);
ALTER TABLE courses ADD COLUMN promotional_discount_percentage INTEGER;
