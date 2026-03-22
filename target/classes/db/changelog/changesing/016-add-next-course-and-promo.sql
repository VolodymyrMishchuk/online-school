-- Add promotional_discount and next_course_id to courses table
ALTER TABLE courses
    ADD COLUMN promotional_discount DECIMAL(5,2),
    ADD COLUMN next_course_id UUID;

-- Add foreign key constraint for next_course_id
ALTER TABLE courses
    ADD CONSTRAINT fk_courses_next_course
        FOREIGN KEY (next_course_id)
            REFERENCES courses(id)
            ON DELETE SET NULL;
