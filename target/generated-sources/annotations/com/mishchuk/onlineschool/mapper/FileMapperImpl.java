package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.FileDto;
import com.mishchuk.onlineschool.repository.entity.FileEntity;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-24T12:00:37+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 23.0.2 (Oracle Corporation)"
)
@Component
public class FileMapperImpl implements FileMapper {

    @Override
    public FileDto toDto(FileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        FileDto fileDto = new FileDto();

        fileDto.setId( entity.getId() );
        fileDto.setFileName( entity.getFileName() );
        fileDto.setOriginalName( entity.getOriginalName() );
        fileDto.setContentType( entity.getContentType() );
        fileDto.setFileSize( entity.getFileSize() );
        fileDto.setUploadedAt( entity.getUploadedAt() );
        fileDto.setRelatedEntityType( entity.getRelatedEntityType() );
        fileDto.setRelatedEntityId( entity.getRelatedEntityId() );

        fileDto.setDownloadUrl( "/files/" + entity.getId() );

        return fileDto;
    }
}
