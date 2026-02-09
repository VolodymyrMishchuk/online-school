package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring", uses = { EnrollmentMapper.class })
public interface PersonMapper {
    PersonDto toDto(PersonEntity entity);

    com.mishchuk.onlineschool.controller.dto.PersonWithEnrollmentsDto toDtoWithEnrollments(PersonEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    PersonEntity toEntity(PersonCreateDto dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enrollments", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntityFromDto(PersonUpdateDto dto, @MappingTarget PersonEntity entity);
}
