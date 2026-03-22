package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.repository.entity.FileEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T08:15:50+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class FileMapperImpl implements FileMapper {

    @Override
    public FileDto toDto(FileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        FileDto fileDto = new FileDto();

        fileDto.setContentType( entity.getContentType() );
        fileDto.setFileName( entity.getFileName() );
        fileDto.setFileSize( entity.getFileSize() );
        fileDto.setId( entity.getId() );
        fileDto.setOriginalName( entity.getOriginalName() );
        fileDto.setRelatedEntityId( entity.getRelatedEntityId() );
        fileDto.setRelatedEntityType( entity.getRelatedEntityType() );
        fileDto.setUploadedAt( entity.getUploadedAt() );

        fileDto.setDownloadUrl( "/files/" + entity.getId() );

        return fileDto;
    }
}
