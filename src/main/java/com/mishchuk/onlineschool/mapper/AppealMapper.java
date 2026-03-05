package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.AppealResponse;
import com.mishchuk.onlineschool.repository.entity.AppealEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AppealMapper {

    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "user.firstName", target = "userFirstName")
    @Mapping(source = "user.lastName", target = "userLastName")
    @Mapping(source = "user.email", target = "userEmail")
    @Mapping(target = "photos", ignore = true)
    AppealResponse toDto(AppealEntity entity);
}
