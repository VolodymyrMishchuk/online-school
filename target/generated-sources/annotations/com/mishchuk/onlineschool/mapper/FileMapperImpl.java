package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.repository.entity.FileEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-29T19:02:08+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class FileMapperImpl implements FileMapper {

    @Override
    public FileDto toDto(FileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        FileDto.FileDtoBuilder fileDto = FileDto.builder();

        fileDto.contentType( entity.getContentType() );
        fileDto.fileName( entity.getFileName() );
        fileDto.fileSize( entity.getFileSize() );
        fileDto.id( entity.getId() );
        fileDto.originalName( entity.getOriginalName() );
        fileDto.relatedEntityId( entity.getRelatedEntityId() );
        fileDto.relatedEntityType( entity.getRelatedEntityType() );
        fileDto.uploadedAt( entity.getUploadedAt() );

        fileDto.downloadUrl( "/files/" + entity.getId() );

        return fileDto.build();
    }
}
