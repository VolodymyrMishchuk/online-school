CREATE TABLE courses (
    id UUID PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    modules_number INTEGER,
    status VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
