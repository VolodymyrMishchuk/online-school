package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ModuleMapper {
    ModuleDto toDto(ModuleEntity entity);

    ModuleEntity toEntity(ModuleCreateDto dto);

    void updateEntityFromDto(ModuleUpdateDto dto, @MappingTarget ModuleEntity entity);
}
