-- changeset antigravity:027-add-course-version-and-update-status

-- Create version column
ALTER TABLE courses ADD COLUMN version VARCHAR(50) DEFAULT '1.0';

-- Update existing statuses to PUBLISHED if they are null, to maintain backwards compatibility
UPDATE courses SET status = 'PUBLISHED' WHERE status IS NULL;
