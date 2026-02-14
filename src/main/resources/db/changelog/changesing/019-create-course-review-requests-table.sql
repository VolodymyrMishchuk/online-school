-- liquibase formatted sql

-- changeset vova:019-create-course-review-requests-table
CREATE TABLE course_review_requests (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    course_id UUID NOT NULL,
    video_url VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'PENDING',
    CONSTRAINT fk_crr_user FOREIGN KEY (user_id) REFERENCES persons(id),
    CONSTRAINT fk_crr_course FOREIGN KEY (course_id) REFERENCES courses(id)
);
