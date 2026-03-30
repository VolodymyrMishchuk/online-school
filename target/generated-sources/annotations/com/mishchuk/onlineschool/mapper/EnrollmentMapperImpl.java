package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.EnrollmentCreateDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-29T19:02:08+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class EnrollmentMapperImpl implements EnrollmentMapper {

    @Override
    public EnrollmentDto toDto(EnrollmentEntity entity) {
        if ( entity == null ) {
            return null;
        }

        UUID studentId = null;
        UUID courseId = null;
        String courseName = null;
        UUID id = null;
        String status = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        studentId = entityStudentId( entity );
        courseId = entityCourseId( entity );
        courseName = entityCourseName( entity );
        id = entity.getId();
        status = entity.getStatus();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        EnrollmentDto enrollmentDto = new EnrollmentDto( id, studentId, courseId, courseName, status, createdAt, updatedAt );

        return enrollmentDto;
    }

    @Override
    public EnrollmentEntity toEntity(EnrollmentCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        EnrollmentEntity enrollmentEntity = new EnrollmentEntity();

        enrollmentEntity.setStudent( enrollmentCreateDtoToPersonEntity( dto ) );
        enrollmentEntity.setCourse( enrollmentCreateDtoToCourseEntity( dto ) );

        enrollmentEntity.setStatus( "ACTIVE" );

        return enrollmentEntity;
    }

    private UUID entityStudentId(EnrollmentEntity enrollmentEntity) {
        if ( enrollmentEntity == null ) {
            return null;
        }
        PersonEntity student = enrollmentEntity.getStudent();
        if ( student == null ) {
            return null;
        }
        UUID id = student.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private UUID entityCourseId(EnrollmentEntity enrollmentEntity) {
        if ( enrollmentEntity == null ) {
            return null;
        }
        CourseEntity course = enrollmentEntity.getCourse();
        if ( course == null ) {
            return null;
        }
        UUID id = course.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityCourseName(EnrollmentEntity enrollmentEntity) {
        if ( enrollmentEntity == null ) {
            return null;
        }
        CourseEntity course = enrollmentEntity.getCourse();
        if ( course == null ) {
            return null;
        }
        String name = course.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    protected PersonEntity enrollmentCreateDtoToPersonEntity(EnrollmentCreateDto enrollmentCreateDto) {
        if ( enrollmentCreateDto == null ) {
            return null;
        }

        PersonEntity personEntity = new PersonEntity();

        personEntity.setId( enrollmentCreateDto.studentId() );

        return personEntity;
    }

    protected CourseEntity enrollmentCreateDtoToCourseEntity(EnrollmentCreateDto enrollmentCreateDto) {
        if ( enrollmentCreateDto == null ) {
            return null;
        }

        CourseEntity courseEntity = new CourseEntity();

        courseEntity.setId( enrollmentCreateDto.courseId() );

        return courseEntity;
    }
}
