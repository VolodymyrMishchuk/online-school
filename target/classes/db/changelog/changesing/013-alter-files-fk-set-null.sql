--liquibase formatted sql

--changeset author:mishchuk
--comment: Change files uploaded_by FK to SET NULL instead of RESTRICT/CASCADE

ALTER TABLE files DROP CONSTRAINT files_uploaded_by_fkey;

ALTER TABLE files
    ADD CONSTRAINT fk_files_uploaded_by
    FOREIGN KEY (uploaded_by)
    REFERENCES persons(id)
    ON DELETE SET NULL;
