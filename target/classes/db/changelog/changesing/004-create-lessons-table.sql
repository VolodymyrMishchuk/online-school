CREATE TABLE lessons (
    id UUID PRIMARY KEY,
    module_id UUID NOT NULL,
    name VARCHAR(255),
    description TEXT,
    video_url VARCHAR(500),
    duration_minutes INTEGER,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_lessons_module FOREIGN KEY (module_id) REFERENCES modules(id)
);
