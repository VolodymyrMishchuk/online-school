-- liquibase formatted sql

-- changeset onlineschool:032-alter-promo-codes-add-status-updated-at
ALTER TABLE promo_codes
ADD COLUMN status_updated_at TIMESTAMP;

UPDATE promo_codes SET status_updated_at = created_at WHERE status_updated_at IS NULL;
