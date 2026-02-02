package com.mishchuk.onlineschool.service;

import com.mishchuk.onlineschool.controller.dto.ModuleCreateDto;
import com.mishchuk.onlineschool.controller.dto.ModuleDto;
import com.mishchuk.onlineschool.controller.dto.ModuleUpdateDto;
import com.mishchuk.onlineschool.mapper.ModuleMapper;
import com.mishchuk.onlineschool.repository.ModuleRepository;
import com.mishchuk.onlineschool.repository.entity.ModuleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final ModuleMapper moduleMapper;

    @Override
    @Transactional
    public void createModule(ModuleCreateDto dto) {
        ModuleEntity entity = moduleMapper.toEntity(dto);
        moduleRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ModuleDto> getModule(java.util.UUID id) {
        return moduleRepository.findById(id)
                .map(moduleMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleDto> getAllModules() {
        return moduleRepository.findAll().stream()
                .map(moduleMapper::toDto)
                .toList();
    }

    @Override
    @Transactional
    public void updateModule(java.util.UUID id, ModuleUpdateDto dto) {
        ModuleEntity entity = moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module not found"));
        moduleMapper.updateEntity(entity, dto);
        moduleRepository.save(entity);
    }

    @Override
    @Transactional
    public void deleteModule(java.util.UUID id) {
        moduleRepository.deleteById(id);
    }
}
