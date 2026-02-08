package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.LessonDto;
import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;

import java.util.List;
import java.util.Optional;

public interface ModuleService {
    void createModule(ModuleCreateDto dto);

    Optional<ModuleDto> getModule(java.util.UUID id);

    List<ModuleDto> getAllModules();

    List<LessonDto> getModuleLessons(java.util.UUID moduleId);

    void updateModule(java.util.UUID id, ModuleUpdateDto dto);

    void deleteModule(java.util.UUID id);
}
