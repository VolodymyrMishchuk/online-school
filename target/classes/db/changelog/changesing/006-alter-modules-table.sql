-- Add course_id column
ALTER TABLE modules ADD COLUMN course_id UUID;

-- Add Foreign Key
ALTER TABLE modules ADD CONSTRAINT fk_modules_course FOREIGN KEY (course_id) REFERENCES courses(id);

-- Drop old course column (assuming we don't care about migrating string data for now)
ALTER TABLE modules DROP COLUMN course;
