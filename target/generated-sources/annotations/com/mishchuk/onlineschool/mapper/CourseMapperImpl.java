package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.CourseCreateDto;
import com.mishchuk.onlineschool.controller.dto.CourseDto;
import com.mishchuk.onlineschool.controller.dto.CourseUpdateDto;
import com.mishchuk.onlineschool.controller.dto.CreatedByDto;
import com.mishchuk.onlineschool.repository.entity.CourseCoverEntity;
import com.mishchuk.onlineschool.repository.entity.CourseEntity;
import com.mishchuk.onlineschool.repository.entity.CourseStatus;
import java.math.BigDecimal;
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
public class CourseMapperImpl implements CourseMapper {

    @Override
    public CourseDto toDto(CourseEntity entity) {
        if ( entity == null ) {
            return null;
        }

        UUID nextCourseId = null;
        String nextCourseName = null;
        String averageColor = null;
        UUID id = null;
        String name = null;
        String description = null;
        String status = null;
        String version = null;
        BigDecimal price = null;
        BigDecimal discountAmount = null;
        Integer discountPercentage = null;
        Integer accessDuration = null;
        Integer promotionalDiscountPercentage = null;
        BigDecimal promotionalDiscountAmount = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        nextCourseId = entityNextCourseId( entity );
        nextCourseName = entityNextCourseName( entity );
        averageColor = entityCoverImageAverageColor( entity );
        id = entity.getId();
        name = entity.getName();
        description = entity.getDescription();
        if ( entity.getStatus() != null ) {
            status = entity.getStatus().name();
        }
        version = entity.getVersion();
        price = entity.getPrice();
        discountAmount = entity.getDiscountAmount();
        discountPercentage = entity.getDiscountPercentage();
        accessDuration = entity.getAccessDuration();
        promotionalDiscountPercentage = entity.getPromotionalDiscountPercentage();
        promotionalDiscountAmount = entity.getPromotionalDiscountAmount();
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        Boolean isEnrolled = false;
        OffsetDateTime enrolledAt = null;
        String enrollmentStatus = null;
        OffsetDateTime expiresAt = null;
        Integer modulesNumber = entity.getModules() != null ? entity.getModules().size() : 0;
        Integer lessonsCount = calculateLessonsCount(entity);
        Integer durationMinutes = calculateDuration(entity);
        String coverImageUrl = entity.getCoverImage() != null ? "/courses/" + entity.getId() + "/cover" : null;
        CreatedByDto createdBy = entity.getCreatedBy() != null ? new com.mishchuk.onlineschool.controller.dto.CreatedByDto(entity.getCreatedBy().getId(), entity.getCreatedBy().getFirstName(), entity.getCreatedBy().getLastName(), entity.getCreatedBy().getEmail()) : null;

        CourseDto courseDto = new CourseDto( id, name, description, modulesNumber, lessonsCount, durationMinutes, status, version, price, discountAmount, discountPercentage, accessDuration, promotionalDiscountPercentage, promotionalDiscountAmount, nextCourseId, nextCourseName, createdAt, updatedAt, isEnrolled, enrolledAt, enrollmentStatus, coverImageUrl, expiresAt, averageColor, createdBy );

        return courseDto;
    }

    @Override
    public CourseEntity toEntity(CourseCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        CourseEntity courseEntity = new CourseEntity();

        courseEntity.setAccessDuration( dto.accessDuration() );
        courseEntity.setDescription( dto.description() );
        courseEntity.setDiscountAmount( dto.discountAmount() );
        courseEntity.setDiscountPercentage( dto.discountPercentage() );
        courseEntity.setName( dto.name() );
        courseEntity.setPrice( dto.price() );
        courseEntity.setPromotionalDiscountAmount( dto.promotionalDiscountAmount() );
        courseEntity.setPromotionalDiscountPercentage( dto.promotionalDiscountPercentage() );

        courseEntity.setStatus( CourseStatus.DRAFT );
        courseEntity.setVersion( "1.0" );

        return courseEntity;
    }

    @Override
    public void updateEntityFromDto(CourseUpdateDto dto, CourseEntity entity) {
        if ( dto == null ) {
            return;
        }

        entity.setAccessDuration( dto.accessDuration() );
        entity.setDescription( dto.description() );
        entity.setDiscountAmount( dto.discountAmount() );
        entity.setDiscountPercentage( dto.discountPercentage() );
        entity.setName( dto.name() );
        entity.setPrice( dto.price() );
        entity.setPromotionalDiscountAmount( dto.promotionalDiscountAmount() );
        entity.setPromotionalDiscountPercentage( dto.promotionalDiscountPercentage() );
    }

    private UUID entityNextCourseId(CourseEntity courseEntity) {
        if ( courseEntity == null ) {
            return null;
        }
        CourseEntity nextCourse = courseEntity.getNextCourse();
        if ( nextCourse == null ) {
            return null;
        }
        UUID id = nextCourse.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityNextCourseName(CourseEntity courseEntity) {
        if ( courseEntity == null ) {
            return null;
        }
        CourseEntity nextCourse = courseEntity.getNextCourse();
        if ( nextCourse == null ) {
            return null;
        }
        String name = nextCourse.getName();
        if ( name == null ) {
            return null;
        }
        return name;
    }

    private String entityCoverImageAverageColor(CourseEntity courseEntity) {
        if ( courseEntity == null ) {
            return null;
        }
        CourseCoverEntity coverImage = courseEntity.getCoverImage();
        if ( coverImage == null ) {
            return null;
        }
        String averageColor = coverImage.getAverageColor();
        if ( averageColor == null ) {
            return null;
        }
        return averageColor;
    }
}
