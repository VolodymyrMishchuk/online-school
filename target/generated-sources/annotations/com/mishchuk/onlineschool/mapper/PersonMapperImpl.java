package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.CreatedByDto;
import com.mishchuk.onlineschool.controller.dto.EnrollmentDto;
import com.mishchuk.onlineschool.controller.dto.PersonCreateDto;
import com.mishchuk.onlineschool.controller.dto.PersonDto;
import com.mishchuk.onlineschool.controller.dto.PersonUpdateDto;
import com.mishchuk.onlineschool.controller.dto.PersonWithEnrollmentsDto;
import com.mishchuk.onlineschool.repository.entity.EnrollmentEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import com.mishchuk.onlineschool.repository.entity.PersonRole;
import com.mishchuk.onlineschool.repository.entity.PersonStatus;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-22T08:15:50+0100",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class PersonMapperImpl implements PersonMapper {

    @Autowired
    private EnrollmentMapper enrollmentMapper;

    @Override
    public PersonDto toDto(PersonEntity entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String firstName = null;
        String lastName = null;
        OffsetDateTime bornedAt = null;
        String phoneNumber = null;
        String email = null;
        String language = null;
        String role = null;
        String status = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        id = entity.getId();
        firstName = entity.getFirstName();
        lastName = entity.getLastName();
        bornedAt = entity.getBornedAt();
        phoneNumber = entity.getPhoneNumber();
        email = entity.getEmail();
        language = entity.getLanguage();
        if ( entity.getRole() != null ) {
            role = entity.getRole().name();
        }
        if ( entity.getStatus() != null ) {
            status = entity.getStatus().name();
        }
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        CreatedByDto createdBy = entity.getCreatedBy() != null ? new com.mishchuk.onlineschool.controller.dto.CreatedByDto(entity.getCreatedBy().getId(), entity.getCreatedBy().getFirstName(), entity.getCreatedBy().getLastName(), entity.getCreatedBy().getEmail()) : null;

        PersonDto personDto = new PersonDto( id, firstName, lastName, bornedAt, phoneNumber, email, language, role, status, createdAt, updatedAt, createdBy );

        return personDto;
    }

    @Override
    public PersonWithEnrollmentsDto toDtoWithEnrollments(PersonEntity entity) {
        if ( entity == null ) {
            return null;
        }

        UUID id = null;
        String firstName = null;
        String lastName = null;
        OffsetDateTime bornedAt = null;
        String phoneNumber = null;
        String email = null;
        String language = null;
        String role = null;
        String status = null;
        List<EnrollmentDto> enrollments = null;
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;

        id = entity.getId();
        firstName = entity.getFirstName();
        lastName = entity.getLastName();
        bornedAt = entity.getBornedAt();
        phoneNumber = entity.getPhoneNumber();
        email = entity.getEmail();
        language = entity.getLanguage();
        if ( entity.getRole() != null ) {
            role = entity.getRole().name();
        }
        if ( entity.getStatus() != null ) {
            status = entity.getStatus().name();
        }
        enrollments = enrollmentEntityListToEnrollmentDtoList( entity.getEnrollments() );
        createdAt = entity.getCreatedAt();
        updatedAt = entity.getUpdatedAt();

        CreatedByDto createdBy = entity.getCreatedBy() != null ? new com.mishchuk.onlineschool.controller.dto.CreatedByDto(entity.getCreatedBy().getId(), entity.getCreatedBy().getFirstName(), entity.getCreatedBy().getLastName(), entity.getCreatedBy().getEmail()) : null;

        PersonWithEnrollmentsDto personWithEnrollmentsDto = new PersonWithEnrollmentsDto( id, firstName, lastName, bornedAt, phoneNumber, email, language, role, status, enrollments, createdAt, updatedAt, createdBy );

        return personWithEnrollmentsDto;
    }

    @Override
    public PersonEntity toEntity(PersonCreateDto dto) {
        if ( dto == null ) {
            return null;
        }

        PersonEntity personEntity = new PersonEntity();

        personEntity.setBornedAt( dto.bornedAt() );
        personEntity.setEmail( dto.email() );
        personEntity.setFirstName( dto.firstName() );
        personEntity.setLanguage( dto.language() );
        personEntity.setLastName( dto.lastName() );
        personEntity.setPassword( dto.password() );
        personEntity.setPhoneNumber( dto.phoneNumber() );

        return personEntity;
    }

    @Override
    public void updateEntityFromDto(PersonUpdateDto dto, PersonEntity entity) {
        if ( dto == null ) {
            return;
        }

        if ( dto.bornedAt() != null ) {
            entity.setBornedAt( dto.bornedAt() );
        }
        if ( dto.email() != null ) {
            entity.setEmail( dto.email() );
        }
        if ( dto.firstName() != null ) {
            entity.setFirstName( dto.firstName() );
        }
        if ( dto.language() != null ) {
            entity.setLanguage( dto.language() );
        }
        if ( dto.lastName() != null ) {
            entity.setLastName( dto.lastName() );
        }
        if ( dto.phoneNumber() != null ) {
            entity.setPhoneNumber( dto.phoneNumber() );
        }
        if ( dto.role() != null ) {
            entity.setRole( Enum.valueOf( PersonRole.class, dto.role() ) );
        }
        if ( dto.status() != null ) {
            entity.setStatus( Enum.valueOf( PersonStatus.class, dto.status() ) );
        }
    }

    protected List<EnrollmentDto> enrollmentEntityListToEnrollmentDtoList(List<EnrollmentEntity> list) {
        if ( list == null ) {
            return null;
        }

        List<EnrollmentDto> list1 = new ArrayList<EnrollmentDto>( list.size() );
        for ( EnrollmentEntity enrollmentEntity : list ) {
            list1.add( enrollmentMapper.toDto( enrollmentEntity ) );
        }

        return list1;
    }
}
