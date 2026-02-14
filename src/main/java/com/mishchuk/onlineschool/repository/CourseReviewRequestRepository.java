package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.CourseReviewRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CourseReviewRequestRepository extends JpaRepository<CourseReviewRequestEntity, UUID> {
}
