package com.mishchuk.onlineschool.repository;

import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<EnrollmentEntity, UUID> {
    List<EnrollmentEntity> findByStudentId(UUID studentId);

    Optional<EnrollmentEntity> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
