CREATE TABLE modules (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    course VARCHAR(255),
    description TEXT,
    lessons_number INTEGER,
    status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
