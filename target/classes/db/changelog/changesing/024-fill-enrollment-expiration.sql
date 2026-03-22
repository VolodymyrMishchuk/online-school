-- liquibase formatted sql

-- changeset volodymyr.mishchuk:024-fill-enrollment-expiration
UPDATE enrollments e
SET expires_at = e.created_at + (c.access_duration * INTERVAL '1 day')
FROM courses c
WHERE e.course_id = c.id
  AND e.expires_at IS NULL
  AND c.access_duration IS NOT NULL
  AND c.access_duration > 0;
