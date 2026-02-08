--liquibase formatted sql

--changeset author:mishchuk
--comment: Create refresh_tokens table for storing refresh tokens

CREATE TABLE refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    token VARCHAR(512) NOT NULL UNIQUE,
    person_id UUID NOT NULL,
    expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_person FOREIGN KEY (person_id) REFERENCES persons(id) ON DELETE CASCADE
);

CREATE INDEX idx_refresh_token_person_id ON refresh_tokens(person_id);
CREATE INDEX idx_refresh_token_expiry ON refresh_tokens(expiry_date);

--rollback DROP TABLE refresh_tokens;
