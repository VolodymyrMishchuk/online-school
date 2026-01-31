package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<CourseEntity, java.util.UUID> {
}
