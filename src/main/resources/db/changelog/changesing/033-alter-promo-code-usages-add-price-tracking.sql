--liquibase formatted sql
--changeset vmishchuk:033-alter-promo-code-usages-add-price-tracking

ALTER TABLE promo_code_usages
    ADD COLUMN discount_type VARCHAR(20),
    ADD COLUMN discount_value NUMERIC(10, 2),
    ADD COLUMN original_price NUMERIC(10, 2),
    ADD COLUMN final_price NUMERIC(10, 2);
