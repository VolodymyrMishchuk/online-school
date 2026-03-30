package com.mishchuk.onlineschool.mapper;

import com.mishchuk.onlineschool.controller.dto.AppealResponse;
import com.mishchuk.onlineschool.repository.entity.AppealEntity;
import com.mishchuk.onlineschool.repository.entity.PersonEntity;
import java.util.UUID;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-03-29T19:02:08+0200",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.45.0.v20260224-0835, environment: Java 21.0.10 (Eclipse Adoptium)"
)
@Component
public class AppealMapperImpl implements AppealMapper {

    @Override
    public AppealResponse toDto(AppealEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AppealResponse appealResponse = new AppealResponse();

        appealResponse.setUserId( entityUserId( entity ) );
        appealResponse.setUserFirstName( entityUserFirstName( entity ) );
        appealResponse.setUserLastName( entityUserLastName( entity ) );
        appealResponse.setUserEmail( entityUserEmail( entity ) );
        appealResponse.setGuestName( entity.getGuestName() );
        appealResponse.setContactDetails( entity.getContactDetails() );
        appealResponse.setContactMethod( entity.getContactMethod() );
        appealResponse.setCreatedAt( entity.getCreatedAt() );
        appealResponse.setId( entity.getId() );
        appealResponse.setMessage( entity.getMessage() );
        appealResponse.setStatus( entity.getStatus() );

        return appealResponse;
    }

    private UUID entityUserId(AppealEntity appealEntity) {
        if ( appealEntity == null ) {
            return null;
        }
        PersonEntity user = appealEntity.getUser();
        if ( user == null ) {
            return null;
        }
        UUID id = user.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String entityUserFirstName(AppealEntity appealEntity) {
        if ( appealEntity == null ) {
            return null;
        }
        PersonEntity user = appealEntity.getUser();
        if ( user == null ) {
            return null;
        }
        String firstName = user.getFirstName();
        if ( firstName == null ) {
            return null;
        }
        return firstName;
    }

    private String entityUserLastName(AppealEntity appealEntity) {
        if ( appealEntity == null ) {
            return null;
        }
        PersonEntity user = appealEntity.getUser();
        if ( user == null ) {
            return null;
        }
        String lastName = user.getLastName();
        if ( lastName == null ) {
            return null;
        }
        return lastName;
    }

    private String entityUserEmail(AppealEntity appealEntity) {
        if ( appealEntity == null ) {
            return null;
        }
        PersonEntity user = appealEntity.getUser();
        if ( user == null ) {
            return null;
        }
        String email = user.getEmail();
        if ( email == null ) {
            return null;
        }
        return email;
    }
}
