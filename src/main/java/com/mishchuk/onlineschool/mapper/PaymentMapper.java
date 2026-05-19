package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.dto.PaymentResponseDto;
import com.mishchuk.onlineschool.repository.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PaymentMapper {

    @Mapping(target = "personId", source = "person.id")
    @Mapping(target = "personName", expression = "java(entity.getPerson() != null ? entity.getPerson().getFirstName() + \" \" + entity.getPerson().getLastName() : null)")
    @Mapping(target = "personEmail", source = "person.email")
    @Mapping(target = "courseId", source = "course.id")
    @Mapping(target = "courseName", source = "course.name")
    @Mapping(target = "accessDurationDays", source = "course.accessDuration")
    PaymentResponseDto toDto(PaymentEntity entity);
}
