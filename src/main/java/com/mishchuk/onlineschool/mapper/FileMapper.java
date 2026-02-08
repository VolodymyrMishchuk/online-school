package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.repository.entity.FileEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FileMapper {

    @Mapping(target = "downloadUrl", expression = "java(\"/api/files/\" + entity.getId())")
    FileDto toDto(FileEntity entity);
}
