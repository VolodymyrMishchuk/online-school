-- Liquibase formatted SQL
-- changeset volodymyr:008-create-files-table

CREATE TABLE files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    file_size BIGINT NOT NULL,
    minio_object_name VARCHAR(500) NOT NULL,
    bucket_name VARCHAR(100) NOT NULL,
    uploaded_by UUID REFERENCES persons(id),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    related_entity_type VARCHAR(50),
    related_entity_id UUID
);

CREATE INDEX idx_files_related_entity ON files(related_entity_type, related_entity_id);
CREATE INDEX idx_files_uploaded_by ON files(uploaded_by);
