package com.mishchuk.onlineschool.controller.dto;

import com.mishchuk.onlineschool.repository.entity.AppealStatus;
import com.mishchuk.onlineschool.repository.entity.ContactMethod;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AppealResponse {
    private UUID id;
    private UUID userId;
    private String userFirstName;
    private String userLastName;
    private String userEmail;

    private ContactMethod contactMethod;
    private String contactDetails;
    private String message;
    private AppealStatus status;

    private OffsetDateTime createdAt;

    private List<FileDto> photos;
}
