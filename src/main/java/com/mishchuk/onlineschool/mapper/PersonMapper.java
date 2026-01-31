package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PersonMapper {
    PersonDto toDto(PersonEntity entity);

    PersonEntity toEntity(PersonCreateDto dto);

    void updateEntityFromDto(PersonUpdateDto dto, @MappingTarget PersonEntity entity);
}
